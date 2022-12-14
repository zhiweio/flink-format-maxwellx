/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.cdc.formats.maxwellx;

import org.apache.flink.annotation.Internal;
import org.apache.flink.api.common.serialization.DeserializationSchema;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.formats.json.JsonRowDataDeserializationSchema;
import org.apache.flink.formats.json.TimestampFormat;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.data.GenericRowData;
import org.apache.flink.table.data.RowData;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.RowType;
import org.apache.flink.types.RowKind;
import org.apache.flink.util.Collector;

import javax.annotation.Nullable;

import java.io.IOException;
import java.util.Objects;

import static java.lang.String.format;
import static org.apache.flink.table.types.utils.TypeConversions.fromLogicalToDataType;

/**
 * Deserialization schema from Maxwell JSON to Flink Table/SQL internal data structure {@link
 * RowData}. The deserialization schema knows Maxwell's schema definition and can extract the
 * database data and convert into {@link RowData} with {@link RowKind}.
 *
 * <p>Deserializes a <code>byte[]</code> message as a JSON object and reads the specified fields.
 *
 * <p>Failures during deserialization are forwarded as wrapped IOExceptions.
 *
 * @see <a href="http://maxwells-daemon.io/">Maxwell</a>
 */
public class MaxwellXJsonDeserializationSchema implements DeserializationSchema<RowData> {
    private static final long serialVersionUID = 1L;

    private static final String OP_INSERT = "insert";
    private static final String OP_UPDATE = "update";
    private static final String OP_DELETE = "delete";

    /** The deserializer to deserialize Maxwell JSON data. */
    private final JsonRowDataDeserializationSchema jsonDeserializer;

    /** TypeInformation of the produced {@link RowData}. * */
    private final TypeInformation<RowData> resultTypeInfo;

    /** Only read changelogs from the specific database. */
    private final @Nullable String database;

    /** Only read changelogs from the specific table. */
    private final @Nullable String table;

    /** Flag indicating the deleted rows contained "old" field, instead of "data" field. */
    private final boolean deletedContainsOldField;

    /** Flag indicating whether to ignore invalid fields/rows (default: throw an exception). */
    private final boolean ignoreParseErrors;

    /** Number of fields. */
    private final int fieldCount;

    public MaxwellXJsonDeserializationSchema(
            RowType rowType,
            TypeInformation<RowData> resultTypeInfo,
            @Nullable String database,
            @Nullable String table,
            boolean deletedContainsOldField,
            boolean ignoreParseErrors,
            TimestampFormat timestampFormatOption) {
        this.resultTypeInfo = resultTypeInfo;
        this.database = database;
        this.table = table;
        this.deletedContainsOldField = deletedContainsOldField;
        this.ignoreParseErrors = ignoreParseErrors;
        this.fieldCount = rowType.getFieldCount();
        this.jsonDeserializer =
                new JsonRowDataDeserializationSchema(
                        createJsonRowType(fromLogicalToDataType(rowType)),
                        // the result type is never used, so it's fine to pass in Canal's result
                        // type
                        resultTypeInfo,
                        false, // ignoreParseErrors already contains the functionality of
                        // failOnMissingField
                        ignoreParseErrors,
                        timestampFormatOption);
    }

    // ------------------------------------------------------------------------------------------
    // Builder
    // ------------------------------------------------------------------------------------------

    /** Creates A builder for building a {@link MaxwellXJsonDeserializationSchema}. */
    public static Builder builder(RowType rowType, TypeInformation<RowData> resultTypeInfo) {
        return new Builder(rowType, resultTypeInfo);
    }

    /** A builder for creating a {@link MaxwellXJsonDeserializationSchema}. */
    @Internal
    public static final class Builder {
        private final RowType rowType;
        private final TypeInformation<RowData> resultTypeInfo;
        private String database = null;
        private String table = null;
        private boolean deletedContainsOldField = false;
        private boolean ignoreParseErrors = false;
        private TimestampFormat timestampFormat = TimestampFormat.SQL;

        private Builder(RowType rowType, TypeInformation<RowData> resultTypeInfo) {
            this.rowType = rowType;
            this.resultTypeInfo = resultTypeInfo;
        }

        public Builder setDatabase(String database) {
            this.database = database;
            return this;
        }

        public Builder setTable(String table) {
            this.table = table;
            return this;
        }

        public Builder setDeletedContainsOldField(boolean deletedContainsOldField) {
            this.deletedContainsOldField = deletedContainsOldField;
            return this;
        }

        public Builder setIgnoreParseErrors(boolean ignoreParseErrors) {
            this.ignoreParseErrors = ignoreParseErrors;
            return this;
        }

        public Builder setTimestampFormat(TimestampFormat timestampFormat) {
            this.timestampFormat = timestampFormat;
            return this;
        }

        public MaxwellXJsonDeserializationSchema build() {
            return new MaxwellXJsonDeserializationSchema(
                    rowType, resultTypeInfo, database, table, deletedContainsOldField, ignoreParseErrors, timestampFormat);
        }
    }

    // ------------------------------------------------------------------------------------------

    @Override
    public RowData deserialize(byte[] message) throws IOException {
        throw new RuntimeException(
                "Please invoke DeserializationSchema#deserialize(byte[], Collector<RowData>) instead.");
    }

    @Override
    public void deserialize(@Nullable byte[] message, Collector<RowData> out) throws IOException {
        if (message == null || message.length == 0) {
            return;
        }
        try {
            RowData row = jsonDeserializer.deserialize(message);
            if (database != null) {
                String currentDatabase = row.getString(3).toString();
                if (!database.equals(currentDatabase)) {
                    return;
                }
            }
            if (table != null) {
                String currentTable = row.getString(4).toString();
                if (!table.equals(currentTable)) {
                    return;
                }
            }
            String type = row.getString(2).toString(); // "type" field
            if (OP_INSERT.equals(type)) {
                // "data" field is a row, contains inserted rows
                RowData insert = row.getRow(0, fieldCount);
                insert.setRowKind(RowKind.INSERT);
                out.collect(insert);
            } else if (OP_UPDATE.equals(type)) {
                // "data" field is a row, contains new rows
                // "old" field is an array of row, contains old values
                // the underlying JSON deserialization schema always produce GenericRowData.
                GenericRowData after = (GenericRowData) row.getRow(0, fieldCount); // "data" field
                GenericRowData before = (GenericRowData) row.getRow(1, fieldCount); // "old" field
                for (int f = 0; f < fieldCount; f++) {
                    if (before.isNullAt(f)) {
                        // not null fields in "old" (before) means the fields are changed
                        // null/empty fields in "old" (before) means the fields are not changed
                        // so we just copy the not changed fields into before
                        before.setField(f, after.getField(f));
                    }
                }
                before.setRowKind(RowKind.UPDATE_BEFORE);
                after.setRowKind(RowKind.UPDATE_AFTER);
                out.collect(before);
                out.collect(after);
            } else if (OP_DELETE.equals(type)) {
                // "data"(or "old") field is a row, contains deleted rows
                RowData delete;
                if (deletedContainsOldField) {
                    // from the "old" field
                    delete = row.getRow(1, fieldCount);
                } else {
                    // from the "data" field
                    delete = row.getRow(0, fieldCount);
                }
                delete.setRowKind(RowKind.DELETE);
                out.collect(delete);

            } else {
                if (!ignoreParseErrors) {
                    throw new IOException(
                            format(
                                    "Unknown \"type\" value \"%s\". The Maxwell JSON message is '%s'",
                                    type, new String(message)));
                }
            }
        } catch (Throwable t) {
            // a big try catch to protect the processing.
            if (!ignoreParseErrors) {
                throw new IOException(
                        format("Corrupt Maxwell JSON message '%s'.", new String(message)), t);
            }
        }
    }

    @Override
    public boolean isEndOfStream(RowData nextElement) {
        return false;
    }

    @Override
    public TypeInformation<RowData> getProducedType() {
        return resultTypeInfo;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        MaxwellXJsonDeserializationSchema that = (MaxwellXJsonDeserializationSchema) o;
        return ignoreParseErrors == that.ignoreParseErrors
                && fieldCount == that.fieldCount
                && Objects.equals(jsonDeserializer, that.jsonDeserializer)
                && Objects.equals(resultTypeInfo, that.resultTypeInfo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(jsonDeserializer, resultTypeInfo, deletedContainsOldField, ignoreParseErrors, fieldCount);
    }

    private RowType createJsonRowType(DataType databaseSchema) {
        // Maxwell JSON contains other information, e.g. "ts"
        // but we don't need them
        return (RowType)
                DataTypes.ROW(
                                DataTypes.FIELD("data", databaseSchema),
                                DataTypes.FIELD("old", databaseSchema),
                                DataTypes.FIELD("type", DataTypes.STRING()),
                                DataTypes.FIELD("database", DataTypes.STRING()),
                                DataTypes.FIELD("table", DataTypes.STRING()))
                        .getLogicalType();
    }
}

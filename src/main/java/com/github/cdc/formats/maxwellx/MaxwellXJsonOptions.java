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

import org.apache.flink.configuration.ConfigOption;
import org.apache.flink.configuration.ConfigOptions;
import org.apache.flink.configuration.ReadableConfig;
import org.apache.flink.formats.json.JsonOptions;

/** Option utils for maxwell-json format. */
public class MaxwellXJsonOptions {

    public static final ConfigOption<Boolean> IGNORE_PARSE_ERRORS = JsonOptions.IGNORE_PARSE_ERRORS;

    public static final ConfigOption<String> TIMESTAMP_FORMAT = JsonOptions.TIMESTAMP_FORMAT;

    public static final ConfigOption<String> JSON_MAP_NULL_KEY_MODE = JsonOptions.MAP_NULL_KEY_MODE;

    public static final ConfigOption<String> JSON_MAP_NULL_KEY_LITERAL =
            JsonOptions.MAP_NULL_KEY_LITERAL;

    public static final ConfigOption<String> DATABASE_INCLUDE =
            ConfigOptions.key("database.include")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Only read changelog rows which match the specific database (by comparing the \"database\" meta field in the record).");

    public static final ConfigOption<String> TABLE_INCLUDE =
            ConfigOptions.key("table.include")
                    .stringType()
                    .noDefaultValue()
                    .withDescription(
                            "Only read changelog rows which match the specific table (by comparing the \"table\" meta field in the record).");

    public static final ConfigOption<Boolean> DELETED_CONTAINS_OLD_FIELD =
            ConfigOptions.key("delete.contains.old-field")
                .booleanType()
                .defaultValue(false)
                .withDescription("Optional flag to specify the deleted rows contained \"old\" field, instead of \"data\" field");

    // --------------------------------------------------------------------------------------------
    // Validation
    // --------------------------------------------------------------------------------------------

    /** Validator for maxwell decoding format. */
    public static void validateDecodingFormatOptions(ReadableConfig tableOptions) {
        JsonOptions.validateDecodingFormatOptions(tableOptions);
    }

    /** Validator for maxwell encoding format. */
    public static void validateEncodingFormatOptions(ReadableConfig tableOptions) {
        JsonOptions.validateEncodingFormatOptions(tableOptions);
    }
}

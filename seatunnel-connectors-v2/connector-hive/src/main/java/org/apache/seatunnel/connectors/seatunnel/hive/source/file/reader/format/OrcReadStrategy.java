/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.seatunnel.connectors.seatunnel.hive.source.file.reader.format;

import org.apache.seatunnel.api.source.Collector;
import org.apache.seatunnel.api.table.type.BasicType;
import org.apache.seatunnel.api.table.type.SeaTunnelDataType;
import org.apache.seatunnel.api.table.type.SeaTunnelRow;
import org.apache.seatunnel.api.table.type.SeaTunnelRowType;
import org.apache.seatunnel.connectors.seatunnel.hive.exception.HivePluginException;
import org.apache.seatunnel.connectors.seatunnel.hive.source.HadoopConf;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.hive.ql.io.orc.OrcFile;
import org.apache.hadoop.hive.ql.io.orc.OrcInputFormat;
import org.apache.hadoop.hive.ql.io.orc.OrcSerde;
import org.apache.hadoop.hive.ql.io.orc.OrcStruct;
import org.apache.hadoop.hive.serde2.objectinspector.StructField;
import org.apache.hadoop.hive.serde2.objectinspector.StructObjectInspector;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.mapred.FileInputFormat;
import org.apache.hadoop.mapred.InputFormat;
import org.apache.hadoop.mapred.InputSplit;
import org.apache.hadoop.mapred.JobConf;
import org.apache.hadoop.mapred.RecordReader;
import org.apache.hadoop.mapred.Reporter;
import org.apache.orc.Reader;
import org.apache.orc.TypeDescription;

import java.io.IOException;
import java.util.List;
import java.util.Properties;

@Slf4j
public class OrcReadStrategy extends AbstractReadStrategy {

    private SeaTunnelRowType seaTunnelRowTypeInfo;

    @Override
    public void read(String path, Collector<SeaTunnelRow> output) throws Exception {
        if (Boolean.FALSE.equals(checkFileType(path))) {
            throw new Exception("please check file type");
        }
        JobConf conf = new JobConf();
        Path filePath = new Path(path);
        Properties p = new Properties();
        OrcSerde serde = new OrcSerde();
        String columns = String.join(",", seaTunnelRowTypeInfo.getFieldNames());
        p.setProperty("columns", columns);
        //support types
        serde.initialize(conf, p);
        StructObjectInspector inspector = (StructObjectInspector) serde.getObjectInspector();
        InputFormat<NullWritable, OrcStruct> in = new OrcInputFormat();
        FileInputFormat.setInputPaths(conf, filePath);
        InputSplit[] splits = in.getSplits(conf, 1);

        conf.set("hive.io.file.readcolumn.ids", "1");
        RecordReader<NullWritable, OrcStruct> reader = in.getRecordReader(splits[0], conf, Reporter.NULL);
        NullWritable key = reader.createKey();
        OrcStruct value = reader.createValue();
        List<? extends StructField> fields = inspector.getAllStructFieldRefs();
        while (reader.next(key, value)) {
            Object[] datas = new Object[fields.size()];
            for (int i = 0; i < fields.size(); i++) {
                Object data = inspector.getStructFieldData(value, fields.get(i));
                if (null != data) {
                    datas[i] = String.valueOf(data);
                } else {
                    datas[i] = null;
                }
            }
            output.collect(new SeaTunnelRow(datas));
        }
        reader.close();
    }

    @Override
    public SeaTunnelRowType getSeaTunnelRowTypeInfo(HadoopConf hadoopConf, String path) throws HivePluginException {

        if (null != seaTunnelRowTypeInfo) {
            return seaTunnelRowTypeInfo;
        }
        Configuration configuration = getConfiguration(hadoopConf);
        Path dstDir = new Path(path);
        Reader reader;
        try {
            reader = OrcFile.createReader(FileSystem.get(configuration), dstDir);
        } catch (IOException e) {
            throw new HivePluginException("Create OrcReader Fail", e);
        }

        TypeDescription schema = reader.getSchema();
        String[] fields = new String[schema.getFieldNames().size()];
        SeaTunnelDataType[] types = new SeaTunnelDataType[schema.getFieldNames().size()];

        for (int i = 0; i < schema.getFieldNames().size(); i++) {
            fields[i] = schema.getFieldNames().get(i);
            types[i] = BasicType.STRING_TYPE;
        }
        seaTunnelRowTypeInfo = new SeaTunnelRowType(fields, types);
        return seaTunnelRowTypeInfo;
    }

}


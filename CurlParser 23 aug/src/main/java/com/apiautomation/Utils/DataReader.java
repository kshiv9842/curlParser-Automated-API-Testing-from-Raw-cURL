package com.apiautomation.Utils;

import static java.lang.System.getProperty;
import static java.lang.System.getenv;
import static java.util.Optional.ofNullable;

import java.nio.file.Path;

import lombok.Getter;
import com.apiautomation.Utils.JsonUtil;

@Getter
public class DataReader {

    public static DataReaderProps readDataReader () {
        DataReaderProps dataReaderprops = null;
        if (dataReaderprops == null) {
            final var defaultPath = Path.of (getProperty ("user.dir"), "src/main/resources/")
                    .toString ();
            String configDirectory = ofNullable (getenv ("LOGIN_PROPS_PATH")).orElse (
                    ofNullable (getProperty ("login.props.path")).orElse (defaultPath));
            final var configPath = Path.of (configDirectory, "datareader_props.json")
                    .toString ();
            dataReaderprops = JsonUtil.fromFile (configPath, DataReaderProps.class);
        }
        return dataReaderprops;
    }
}

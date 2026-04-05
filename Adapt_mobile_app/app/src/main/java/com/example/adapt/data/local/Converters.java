package com.example.adapt.data.local;

import androidx.room.TypeConverter;
import com.example.adapt.data.model.ActivityLog;

public class Converters {
    @TypeConverter
    public static String fromSource(ActivityLog.Source source) {
        return source == null ? null : source.name();
    }

    @TypeConverter
    public static ActivityLog.Source toSource(String source) {
        return source == null ? null : ActivityLog.Source.valueOf(source);
    }

    @TypeConverter
    public static String fromType(ActivityLog.Type type) {
        return type == null ? null : type.name();
    }

    @TypeConverter
    public static ActivityLog.Type toType(String type) {
        return type == null ? null : ActivityLog.Type.valueOf(type);
    }
}

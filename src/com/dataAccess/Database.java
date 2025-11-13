package com.dataAccess;

import java.io.IOException;
import java.util.List;

public interface Database {
    void append(CalendarDTO dto) throws IOException;

    List<CalendarDTO> readAll() throws IOException;

    void rewriteAll(List<CalendarDTO> all) throws IOException;
}

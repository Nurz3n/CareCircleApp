package com.carecircleserver.calendarSystem;

import com.dataAccess.CalendarDAO;
import com.dataAccess.CsvCalendarDAO;
import com.dataAccess.CsvCalendarDatabase;
import com.dataAccess.Database;

public final class CalendarWiring {
    private static final ServiceDispatcher DISPATCHER = build();

    private static ServiceDispatcher build() {
        Database db = new CsvCalendarDatabase();
        CalendarDAO dao = new CsvCalendarDAO(db);
        CalendarService svc = new DefaultCalendarService(dao);
        return new CalendarServiceDispatcher(svc);
    }

    public static ServiceDispatcher dispatcher() {
        return DISPATCHER;
    }

    private CalendarWiring() {
    }
}

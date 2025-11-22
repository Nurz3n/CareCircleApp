package com.carecircleserver.calendarSystem;

public interface ServiceRequest<R> {
    R handleWith(CalendarService service);
}



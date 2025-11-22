package com.carecircleserver.calendarSystem;

public final class CalendarServiceDispatcher implements ServiceDispatcher {
    private final CalendarService service;
    public CalendarServiceDispatcher(CalendarService service) { this.service = service; }
    @Override public <R> R dispatch(ServiceRequest<R> request) { return request.handleWith(service); }
}
package com.servifood.presentation.rest.dto;
import java.time.DayOfWeek;
import java.time.LocalTime;
public record BusinessHoursResponse(DayOfWeek dayOfWeek, int slotNumber, LocalTime opensAt, LocalTime closesAt, boolean closed) {}

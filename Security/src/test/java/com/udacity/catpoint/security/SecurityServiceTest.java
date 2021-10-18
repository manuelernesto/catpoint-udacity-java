package com.udacity.catpoint.security;

import com.udacity.catpoint.image.service.FakeImageService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService service;

    private Sensor sensor;

    private final String uuid = UUID.randomUUID().toString();

    @Mock
    private StatusListener statusListener;

    @Mock
    private FakeImageService imageService;

    @Mock
    private SecurityRepository repository;

    private Set<Sensor> getAllSensors(int count, boolean status) {
        var sensors =
                IntStream
                        .range(0, count)
                        .mapToObj(i -> new Sensor(uuid, SensorType.DOOR))
                        .collect(Collectors.toCollection(HashSet::new));

        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

    private Sensor getSensor() {
        return new Sensor(uuid, SensorType.DOOR);
    }

    @BeforeEach
    void setup() {
        service = new SecurityService(repository, imageService);
        sensor = getSensor();
    }

    @Test
    void ifSystemArmedAndSensorActivatedAndPendingState_changeStatusToAlarm() {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        service.changeSensorActivationStatus(sensor, true);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }
}

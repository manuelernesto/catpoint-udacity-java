package com.udacity.catpoint.security;

import com.udacity.catpoint.image.service.IService;
import com.udacity.catpoint.security.application.StatusListener;
import com.udacity.catpoint.security.data.*;
import com.udacity.catpoint.security.service.SecurityService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityServiceTest {

    private SecurityService service;

    private Sensor sensor;

    private final String uuid = UUID.randomUUID().toString();

    @Mock
    private StatusListener statusListener;

    @Mock
    private IService imageService;

    @Mock
    private SecurityRepository repository;

    @BeforeEach
    void setup() {
        service = new SecurityService(repository, imageService);
        sensor = getSensor();
    }


    // Tests required as a functional requirement


    @Test
    void ifSystemArmedAndSensorActivated_changeStatusToPending() {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.NO_ALARM);
        service.changeSensorActivationStatus(sensor, true);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }

    @Test
    void ifSystemArmedAndSensorActivatedAndPendingState_changeStatusToAlarm() {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        service.changeSensorActivationStatus(sensor, true);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifPendingAlarmAndSensorInactive_returnNoAlarmState() {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(false);
        service.changeSensorActivationStatus(sensor);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @ValueSource(booleans = {true, false})
    void ifAlarmIsActive_changeSensorShouldNotAffectAlarmState(boolean status) {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        service.changeSensorActivationStatus(sensor, status);

        verify(repository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifSensorActivatedWhileActiveAndPendingAlarm_changeStatusToAlarm() {
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        sensor.setActive(true);
        service.changeSensorActivationStatus(sensor, true);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM", "ALARM"})
    void ifSensorDeactivatedWhileInactive_noChangesToAlarmState(AlarmStatus status) {
        when(repository.getAlarmStatus()).thenReturn(status);
        sensor.setActive(false);
        service.changeSensorActivationStatus(sensor, false);

        verify(repository, never()).setAlarmStatus(any(AlarmStatus.class));
    }

    @Test
    void ifImageServiceIdentifiesCatWhileAlarmArmedHome_changeStatusToAlarm() {
        var catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.ARMED_HOME);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(true);
        service.processImage(catImage);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }

    @Test
    void ifImageServiceIdentifiesNoCatImage_changeStatusToNoAlarmAsLongSensorsNotActive() {
        Set<Sensor> sensors = getAllSensors(3, false);
        when(repository.getSensors()).thenReturn(sensors);
        when(imageService.imageContainsCat(any(), ArgumentMatchers.anyFloat())).thenReturn(false);
        service.processImage(mock(BufferedImage.class));

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @Test
    void ifSystemDisarmed_setNoAlarmState() {
        service.setArmingStatus(ArmingStatus.DISARMED);
        verify(repository, times(1)).setAlarmStatus(AlarmStatus.NO_ALARM);
    }

    @ParameterizedTest
    @EnumSource(value = ArmingStatus.class, names = {"ARMED_HOME", "ARMED_AWAY"})
    void ifSystemArmed_resetSensorsToInactive(ArmingStatus status) {
        Set<Sensor> sensors = getAllSensors(3, true);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.PENDING_ALARM);
        when(repository.getSensors()).thenReturn(sensors);
        service.setArmingStatus(status);

        service.getSensors().forEach(sensor -> {
            assertFalse(sensor.getActive());
        });
    }

    @Test
    void ifSystemArmedHomeWhileImageServiceIdentifiesCat_changeStatusToAlarm() {
        BufferedImage catImage = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
        when(imageService.imageContainsCat(any(), anyFloat())).thenReturn(true);
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        service.processImage(catImage);
        service.setArmingStatus(ArmingStatus.ARMED_HOME);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.ALARM);
    }


    //Extra tests for the SecurityService class

    /*
     * Status Listener Test
     * */
    @Test
    void addAndRemoveStatusListener() {
        service.addStatusListener(statusListener);
        service.removeStatusListener(statusListener);
    }

    /*
     * Sensor Listener test
     * */
    @Test
    void addAndRemoveSensor() {
        service.addSensor(sensor);
        service.removeSensor(sensor);
    }

    /*
     * System disarmed Test
     * */
    @ParameterizedTest
    @EnumSource(value = AlarmStatus.class, names = {"NO_ALARM", "PENDING_ALARM"})
    void ifSystemDisarmedAndSensorActivated_noChangesToArmingState(AlarmStatus status) {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(repository.getAlarmStatus()).thenReturn(status);
        service.changeSensorActivationStatus(sensor, true);

        verify(repository, never()).setArmingStatus(ArmingStatus.DISARMED);
    }

    /*
     * System deactivated and Alarm state test
     * */
    @Test
    void ifAlarmStateAndSystemDisarmed_changeStatusToPending() {
        when(repository.getArmingStatus()).thenReturn(ArmingStatus.DISARMED);
        when(repository.getAlarmStatus()).thenReturn(AlarmStatus.ALARM);
        service.changeSensorActivationStatus(sensor);

        verify(repository, times(1)).setAlarmStatus(AlarmStatus.PENDING_ALARM);
    }


    // Private methods

    private Sensor getSensor() {
        return new Sensor(uuid, SensorType.DOOR);
    }

    private Set<Sensor> getAllSensors(int count, boolean status) {
        var sensors =
                IntStream
                        .range(0, count)
                        .mapToObj(i -> new Sensor(uuid, SensorType.DOOR))
                        .collect(Collectors.toCollection(HashSet::new));

        sensors.forEach(sensor -> sensor.setActive(status));
        return sensors;
    }

}

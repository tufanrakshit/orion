package com.pinterest.orion.core.global.sensor;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.pinterest.orion.core.PluginConfigurationException;
import com.pinterest.orion.core.configs.PluginConfig;

import io.dropwizard.lifecycle.Managed;

public class GlobalPluginManager implements Managed {

  private static final Logger logger = Logger
      .getLogger(GlobalPluginManager.class.getCanonicalName());
  private static final Map<String, GlobalSensor> MAP = new ConcurrentHashMap<>();
  private ScheduledExecutorService es = null;
  private List<PluginConfig> globalSensorConfigs;

  public void initialize(List<PluginConfig> globalSensorConfigs) throws PluginConfigurationException {
    this.globalSensorConfigs = globalSensorConfigs;
    if (globalSensorConfigs == null) {
      return;
    }
  }

  @Override
  public void start() throws Exception {
    es = Executors.newScheduledThreadPool(2);
    try {
      for (PluginConfig pluginConfig : globalSensorConfigs) {
        Class<? extends GlobalSensor> asSubclass = Class.forName(pluginConfig.getClazz())
            .asSubclass(GlobalSensor.class);
        GlobalSensor sensor = asSubclass.newInstance();
        sensor.initialize(pluginConfig.getConfiguration());
        MAP.put(sensor.getName(), sensor);
        logger.info("Initialzing plugin:" + sensor.getName());
      }
    } catch (Exception e) {
      logger.log(Level.SEVERE, "Failed to initialize plugin", e);
    }
    for (Entry<String, GlobalSensor> entry : MAP.entrySet()) {
      GlobalSensor sensor = entry.getValue();
      es.scheduleAtFixedRate(sensor, sensor.getInterval(), sensor.getInterval(), TimeUnit.SECONDS);
      logger.info(
          "Scheduled sensor:" + sensor.getName() + " every:" + sensor.getInterval() + " seconds");
    }
  }

  @Override
  public void stop() throws Exception {
    if (es != null) {
      es.shutdownNow();
    }
  }

  public static GlobalSensor getSensorInstance(String sensorName) {
    return MAP.get(sensorName);
  }
  
  @VisibleForTesting
  public static void setSensorInstance(String sensorName, GlobalSensor sensor) {
    MAP.put(sensorName, sensor);
  }

  public static Collection<GlobalSensor> listSensors() {
    return MAP.values();
  }
}

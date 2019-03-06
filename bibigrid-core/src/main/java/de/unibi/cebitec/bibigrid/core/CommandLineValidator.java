package de.unibi.cebitec.bibigrid.core;

import de.unibi.cebitec.bibigrid.core.model.*;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.net.InetAddress;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.InstanceTypeNotFoundException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class CommandLineValidator {
    protected static final Logger LOG = LoggerFactory.getLogger(CommandLineValidator.class);
    protected final CommandLine cl;
    protected final List<String> req;
    protected final DefaultPropertiesFile defaultPropertiesFile;
    protected final IntentMode intentMode;
    private final ProviderModule providerModule;
    protected final Configuration config;
    private Configuration.SlaveInstanceConfiguration commandLineSlaveInstance;

    public CommandLineValidator(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        this.cl = cl;
        this.defaultPropertiesFile = defaultPropertiesFile;
        this.intentMode = intentMode;
        this.providerModule = providerModule;
        config = defaultPropertiesFile.loadConfiguration(getProviderConfigurationClass());
        if (config != null && defaultPropertiesFile.isAlternativeFilepath()) {
            config.setAlternativeConfigPath(defaultPropertiesFile.getPropertiesFilePath().toString());
        }
        req = getRequiredOptions();
    }

    /**
     * Create the {@link Configuration} model instance. Must be overridden by provider
     * implementations for specific configuration fields.
     */
    protected abstract Class<? extends Configuration> getProviderConfigurationClass();


    protected final boolean checkRequiredParameter(String shortParam, String value) {
        if (req.contains(shortParam) && isStringNullOrEmpty(value)) {
            LOG.error("-" + shortParam + " option is required!");
            return false;
        }
        return true;
    }

    private boolean parseTerminateParameter() {
        // terminate (cluster-id)
        if (req.contains(IntentMode.TERMINATE.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.TERMINATE.getShortParam()).trim());
        }
        return true;
    }

    private boolean parseCloud9Parameter() {
        // cloud9 (cluster-id)
        if (req.contains(IntentMode.CLOUD9.getShortParam())) {
            config.setClusterIds(cl.getOptionValue(IntentMode.CLOUD9.getShortParam()).trim());
        }
        return true;
    }





























    private static List<Configuration.MountPoint> parseMountCsv(String mountsCsv, String logName) {
        List<Configuration.MountPoint> mountPoints = new ArrayList<>();
        if (mountsCsv != null && !mountsCsv.isEmpty()) {
            try {
                String[] mounts = mountsCsv.split(",");
                for (String mountKeyValue : mounts) {
                    String[] mountSplit = mountKeyValue.trim().split("=");
                    Configuration.MountPoint mountPoint = new Configuration.MountPoint();
                    mountPoint.setSource(mountSplit[0].trim());
                    mountPoint.setTarget(mountSplit[1].trim());
                    mountPoints.add(mountPoint);
                }
            } catch (Exception e) {
                LOG.error("Could not parse the list of {} mounts, please make sure you have a list of " +
                        "comma-separated key=value pairs without spaces in between.", logName);
                return null;
            }
            if (!mountPoints.isEmpty()) {
                StringBuilder mountsDisplay = new StringBuilder();
                for (Configuration.MountPoint mount : mountPoints) {
                    mountsDisplay.append(mount.getSource()).append(" => ").append(mount.getTarget()).append(" ; ");
                }
                LOG.info(V, "{} mounts: {}", logName, mountsDisplay);
            }
        }
        return mountPoints;
    }














    protected abstract List<String> getRequiredOptions();

    protected abstract boolean validateProviderParameters();

    public boolean validateProviderTypes(Client client) {
        try {
            InstanceType masterType = providerModule.getInstanceType(client, config, config.getMasterInstance().getType());
            config.getMasterInstance().setProviderType(masterType);
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid master instance type specified!", e);
            return false;
        }
        try {
            for (Configuration.InstanceConfiguration instanceConfiguration : config.getSlaveInstances()) {
                InstanceType slaveType = providerModule.getInstanceType(client, config, instanceConfiguration.getType());
                instanceConfiguration.setProviderType(slaveType);
            }
        } catch (InstanceTypeNotFoundException e) {
            LOG.error("Invalid slave instance type specified!", e);
            return false;
        }
        return true;
    }

    protected static boolean isStringNullOrEmpty(final String s) {
        return s == null || s.trim().isEmpty();
    }

    private int checkStringAsInt(String s, int max) throws Exception {
        int v = Integer.parseInt(s);
        if (v < 0 || v > max) {
            throw new Exception();
        }
        return v;
    }

    public Configuration getConfig() {
        return config;
    }
}

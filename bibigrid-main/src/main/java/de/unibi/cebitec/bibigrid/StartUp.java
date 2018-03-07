package de.unibi.cebitec.bibigrid;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.intents.CreateCluster;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.intents.ValidateIntent;
import de.unibi.cebitec.bibigrid.core.model.*;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ClientConnectionFailedException;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import de.unibi.cebitec.bibigrid.core.util.RuleBuilder;
import de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter;

import java.io.IOException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Formatter;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.stream.Collectors;

import de.unibi.techfak.bibiserv.cms.Tparam;
import de.unibi.techfak.bibiserv.cms.TparamGroup;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Startup/Main class of BiBiGrid.
 *
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class StartUp {
    private static final Logger LOG = LoggerFactory.getLogger(StartUp.class);
    private static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation. No instances started/terminated.";
    private static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation. Instances already running. " +
            "I will try to shut them down but in case of an error they might remain running. Please check manually " +
            "afterwards.";

    private static OptionGroup getCMDLineOptionGroup() {
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        Option terminate = new Option(IntentMode.TERMINATE.getShortParam(), IntentMode.TERMINATE.getLongParam(),
                true, "Terminate running cluster");
        terminate.setArgName("cluster-id");
        intentOptions
                .addOption(new Option(IntentMode.VERSION.getShortParam(), IntentMode.VERSION.getLongParam(),
                        false, "Version"))
                .addOption(new Option(IntentMode.HELP.getShortParam(), IntentMode.HELP.getLongParam(),
                        false, "Help"))
                .addOption(new Option(IntentMode.CREATE.getShortParam(), IntentMode.CREATE.getLongParam(),
                        false, "Create cluster"))
                .addOption(new Option(IntentMode.PREPARE.getShortParam(), IntentMode.PREPARE.getLongParam(),
                        false, "Prepare cluster images for faster setup"))
                .addOption(new Option(IntentMode.LIST.getShortParam(), IntentMode.LIST.getLongParam(),
                        false, "List running clusters"))
                .addOption(new Option(IntentMode.VALIDATE.getShortParam(), IntentMode.VALIDATE.getLongParam(),
                        false, "Validate the configuration file"))
                .addOption(terminate);
        return intentOptions;
    }

    private static Options getRulesToOptions() {
        RuleBuilder ruleBuild = new RuleBuilder();
        TparamGroup ruleSet = ruleBuild.getRules();
        Options ruleOptions = new Options();
        for (Object ob : ruleSet.getParamrefOrParamGroupref()) {
            Tparam tp = (Tparam) ob;
            boolean hasArg;
            hasArg = tp.getType() != null;
            ruleOptions.addOption(new Option(tp.getId(), tp.getOption(), hasArg, tp.getShortDescription().get(0).getValue()));
        }
        return ruleOptions;
    }

    public static void main(String[] args) {
        CommandLineParser cli = new DefaultParser();
        OptionGroup intentOptions = getCMDLineOptionGroup();
        Options cmdLineOptions = getRulesToOptions();
        cmdLineOptions.addOptionGroup(intentOptions);
        try {
            CommandLine cl = cli.parse(cmdLineOptions, args);
            if (cl.hasOption("v")) {
                VerboseOutputFilter.SHOW_VERBOSE = true;
            }
            IntentMode intentMode = IntentMode.fromString(intentOptions.getSelected());
            switch (intentMode) {
                case VERSION:
                    printVersionInfo();
                    break;
                case HELP:
                    printHelp(cl, cmdLineOptions);
                    break;
                case CREATE:
                case PREPARE:
                case LIST:
                case TERMINATE:
                case VALIDATE:
                    runIntent(cl, intentMode);
                    break;
            }
        } catch (ParseException pe) {
            LOG.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
    }

    private static void printVersionInfo() {
        try {
            URL jarUrl = StartUp.class.getProtectionDomain().getCodeSource().getLocation();
            String jarPath = URLDecoder.decode(jarUrl.getFile(), "UTF-8");
            JarFile jarFile = new JarFile(jarPath);
            Manifest m = jarFile.getManifest();
            System.out.println(String.format("v%s (Build: %s)",
                    m.getMainAttributes().getValue("Bibigrid-version"),
                    m.getMainAttributes().getValue("Bibigrid-build-date")));
        } catch (IOException e) {
            LOG.error("Version info could not be read.");
        }
    }

    private static void printHelp(CommandLine commandLine, Options cmdLineOptions) {
        // TODO: improve help modes
        if (commandLine.hasOption("lit")) {
            runIntent(commandLine, IntentMode.HELP);
            return;
        }
        HelpFormatter help = new HelpFormatter();
        String header = "\nDocumentation at https://github.com/BiBiServ/bibigrid/docs\n\n";
        header += "Loaded provider modules: " + String.join(", ", Provider.getInstance().getProviderNames()) + "\n\n";
        String footer = "";
        String modes = Arrays.stream(IntentMode.values()).map(m -> "--" + m.getLongParam()).collect(Collectors.joining("|"));
        help.printHelp("bibigrid " + modes + " [...]", header, cmdLineOptions, footer);
    }

    private static void runIntent(CommandLine commandLine, IntentMode intentMode) {
        DefaultPropertiesFile defaultPropertiesFile = new DefaultPropertiesFile(commandLine);
        String providerMode = parseProviderMode(commandLine, defaultPropertiesFile);
        if (providerMode == null) {
            LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
            return;
        }
        ProviderModule module = Provider.getInstance().getProviderModule(providerMode);
        if (module == null) {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
            return;
        }
        CommandLineValidator validator = module.getCommandLineValidator(commandLine, defaultPropertiesFile, intentMode);
        if (validator.validate(providerMode)) {
            Client client;
            try {
                client = module.getClient(validator.getConfig());
            } catch (ClientConnectionFailedException e) {
                LOG.error(ABORT_WITH_NOTHING_STARTED, e);
                return;
            }
            // In order to validate the native instance types, we need a client. So this step is deferred after
            // client connection is established.
            if (!validator.validateProviderTypes(client)) {
                LOG.error(ABORT_WITH_NOTHING_STARTED);
            }
            switch (intentMode) {
                case HELP:
                    printInstanceTypeHelp(module, client, validator.getConfig());
                    break;
                case LIST:
                    LOG.info(module.getListIntent(client, validator.getConfig()).toString());
                    break;
                case VALIDATE:
                    ValidateIntent intent = module.getValidateIntent(client, validator.getConfig());
                    if (intent != null) {
                        intent.validate();
                    }
                    break;
                case CREATE:
                    runCreateIntent(module, validator, client, module.getCreateIntent(client, validator.getConfig()), false);
                    break;
                case PREPARE:
                    CreateCluster cluster = module.getCreateIntent(client, validator.getConfig());
                    if (runCreateIntent(module, validator, client, cluster, true)) {
                        module.getPrepareIntent(client, validator.getConfig()).prepare(cluster.getMasterInstance(),
                                cluster.getSlaveInstances());
                        module.getTerminateIntent(client, validator.getConfig()).terminate();
                    }
                    break;
                case TERMINATE:
                    module.getTerminateIntent(client, validator.getConfig()).terminate();
                    break;
                default:
                    break;
            }
        } else {
            LOG.error(ABORT_WITH_NOTHING_STARTED);
        }
    }

    private static boolean runCreateIntent(ProviderModule module, CommandLineValidator validator, Client client,
                                           CreateCluster cluster, boolean prepare) {
        try {
            boolean success = cluster
                    .createClusterEnvironment()
                    .createNetwork()
                    .createSubnet()
                    .createSecurityGroup()
                    .createPlacementGroup()
                    .configureClusterMasterInstance()
                    .configureClusterSlaveInstance()
                    .launchClusterInstances(prepare);
            if (!success) {
                LOG.error(StartUp.ABORT_WITH_INSTANCES_RUNNING);
                TerminateIntent cleanupIntent = module.getTerminateIntent(client, validator.getConfig());
                cleanupIntent.terminate();
                return false;
            }
        } catch (ConfigurationException ex) {
            // print stacktrace only verbose mode, otherwise the message is fine
            if (VerboseOutputFilter.SHOW_VERBOSE) {
                LOG.error("Failed to create cluster. {} {}", ex.getMessage(), ex);
            } else {
                LOG.error("Failed to create cluster. {}", ex.getMessage());
            }
            return false;
        }
        return true;
    }

    private static String parseProviderMode(CommandLine commandLine, DefaultPropertiesFile defaultPropertiesFile) {
        try {
            return commandLine.getOptionValue("mode", defaultPropertiesFile.getPropertiesMode()).trim();
        } catch (IllegalArgumentException iae) {
            LOG.error("No suitable mode found. Exit");
        }
        return null;
    }

    private static void printInstanceTypeHelp(ProviderModule module, Client client, Configuration config) {

        StringBuilder display = new StringBuilder();
        Formatter formatter = new Formatter(display, Locale.US);
        display.append("\n");
        formatter.format("%25s | %5s | %15s | %15s | %4s | %10s%n", "name", "cores", "ram Mb", "disk size Mb", "swap",
                "ephemerals");
        display.append(new String(new char[89]).replace('\0', '-')).append("\n");
        for (InstanceType type : module.getInstanceTypes(client, config)) {
            formatter.format("%25s | %5s | %15s | %15s | %4s | %10s%n", type.getValue(), type.getCpuCores(),
                    type.getMaxRam(), type.getMaxDiskSpace(), type.getSwap(), type.getEphemerals());
        }
        System.out.println(display.toString());
    }
}

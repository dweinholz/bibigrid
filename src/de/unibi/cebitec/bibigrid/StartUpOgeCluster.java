package de.unibi.cebitec.bibigrid;

import com.amazonaws.services.ec2.model.InstanceType;
import de.unibi.cebitec.bibigrid.ctrl.*;
import de.unibi.cebitec.bibigrid.exc.IntentNotConfiguredException;
import de.unibi.cebitec.bibigrid.util.VerboseOutputFilter;
import java.net.URL;
import java.net.URLDecoder;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import org.apache.commons.cli.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class StartUpOgeCluster {

    public static final Logger log = LoggerFactory.getLogger(StartUpOgeCluster.class);
    public static final String ABORT_WITH_NOTHING_STARTED = "Aborting operation."
            + " No instances started/terminated.";
    public static final String ABORT_WITH_INSTANCES_RUNNING = "Aborting operation."
            + " Instances already running. I will try to shut them down but in case of"
            + " an error they might remain running. Please check manually afterwards.";

    public static void main(String[] args) {

//        args = new String[]{"bibigrid", "-c", "-o", "/home/jsteiner/bibigrid.properties", "-vpc", "vpc-3e9d165b", "-me"};
//        args = new String[]{"bibigrid", "-c", "-o", "/homes/jsteiner/bibigrid.properties.os", "-u", "jsteiner"};
//        args = new String[]{"bibigrid", "-t", "46a93722-6790-4d65-97bd-a79d5dae23ae", "-o", "/homes/jsteiner/bibigrid.properties.os", "-u", "jsteiner"};
//        args = new String[]{"bibigrid", "-c", "-o", "/homes/jsteiner/bibigrid.properties.os", "-u", "jsteiner"};
//        args = new String[]{"bibigrid", "-t all", "-o", "/homes/jsteiner/bibigrid.properties.os", "-u", "jsteiner"};
        CommandLineParser cli = new PosixParser();
        OptionGroup intentOptions = new OptionGroup();
        intentOptions.setRequired(true);
        intentOptions
                .addOption(OptionBuilder.withLongOpt("version").withDescription("version").create("V"))
                .addOption(OptionBuilder.withLongOpt("help").withDescription("help").create("h"))
                .addOption(OptionBuilder.withLongOpt("create").withDescription("create cluster").create("c"))
                .addOption(OptionBuilder.withLongOpt("list").withDescription("list running clusters").create("l"))
                .addOption(OptionBuilder.withLongOpt("check").withDescription("check config file").create("ch"))
                .addOption(OptionBuilder.withLongOpt("terminate").withDescription("terminate running cluster").hasArg().withArgName("cluster-id").create("t"));

        Options cmdLineOptions = new Options();
        cmdLineOptions
                .addOptionGroup(intentOptions)
                .addOption("m", "master-instance-type", true, "see INSTANCE-TYPES below")
                .addOption("M", "master-image", true, "AMI for master")
                .addOption("s", "slave-instance-type", true, "see INSTANCE-TYPES below")
                .addOption("n", "slave-instance-count", true, "min: 0")
                .addOption("S", "slave-image", true, "AMI for slaves")
                .addOption("k", "keypair", true, "name of the keypair in aws console")
                .addOption("i", "identity-file", true, "absolute path to private ssh key file")
                .addOption("e", "region", true, "region of instance")
                .addOption("z", "availability-zone", true, "")
                .addOption("ex", "early-execute-script", true, "path to shell script to be executed on master instance startup")
                .addOption("a", "aws-credentials-file", true, "containing access-key-id & secret-key, default: ~/.bibigrid.properties")
                .addOption("p", "ports", true, "comma-separated list of additional ports (tcp & udp) to be opened for all nodes (e.g. 80,443,8080)")
                .addOption("d", "master-mounts", true, "comma-separated snapshot=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to master. (Optional: Partition selection with ':', e.g. snap-12234abcd:1=/mnt/mydir1)")
                .addOption("f", "slave-mounts", true, "comma-separated snapshot=mountpoint list (e.g. snap-12234abcd=/mnt/mydir1,snap-5667889ab=/mnt/mydir2) mounted to all slaves individually")
                .addOption("x", "execute-script", true, "shell script file to be executed on master")
                .addOption("g", "nfs-shares", true, "comma-separated list of paths on master to be shared via NFS")
                .addOption("v", "verbose", false, "more console output")
                .addOption("o", "config", true, "path to alternative config file")
                .addOption("b", "use-master-as-compute", true, "yes or no if master is supposed to be used as a compute instance")
                .addOption("db", "cassandra", false, "Enable Cassandra database support")
                .addOption("gpf", "grid-properties-file", true, "store essential grid properties like master & slave dns values and grid id in a Java property file")
                .addOption("vpc", "vpc-id", true, "Vpc ID used instead of default vpc")
                .addOption("me", "mesos", true, "Yes or no if Mesos framework should be configured/started. Default is No")
                .addOption("meta", "meta-mode", true, "Allows you to use a different cloud provider e.g openstack with meta=openstack. Default AWS is used!")
                .addOption("oge", "oge", true, "Yes or no if OpenGridEngine should be configured/started. Default is Yes!")
                .addOption("nfs", "nfs", true, "Yes or no if NFS should be configured/started. Default is Yes!")
                .addOption("u", "user", true, "User name (mandatory)");

        try {
            CommandLine cl = cli.parse(cmdLineOptions, args);
            CommandLineValidator validator = null;
            Intent intent = null;
            if (cl.hasOption("v")) {
                VerboseOutputFilter.SHOW_VERBOSE = true;
            }
            switch (intentOptions.getSelected()) {
                case "V":
                    try {
                        URL jarUrl = StartUpOgeCluster.class.getProtectionDomain().getCodeSource().getLocation();
                        String jarPath = URLDecoder.decode(jarUrl.getFile(), "UTF-8");
                        JarFile jarFile = new JarFile(jarPath);
                        Manifest m = jarFile.getManifest();
                        StringBuilder versionInfo = new StringBuilder("v");
                        versionInfo.append(m.getMainAttributes().getValue("Bibigrid-version"));
                        versionInfo.append(" (Build: ");
                        versionInfo.append(m.getMainAttributes().getValue("Bibigrid-build-date"));
                        versionInfo.append(")");
                        System.out.println(versionInfo.toString());
                    } catch (Exception e) {
                        log.error("Version info could not be read.");
                    }
                    break;
                case "h":
                    HelpFormatter help = new HelpFormatter();
                    String header = ""; //TODO: infotext (auf default props hinweisen); instanzgroessen auflisten
                    StringBuilder footer = new StringBuilder(" \nValid INSTANCE-TYPES are:");
                    for (InstanceType type : InstanceType.values()) {
                        footer.append("\n-\t");
                        footer.append(type.toString());
                    }
                    footer.append("\n.");
                    help.printHelp("bibigrid --create|--list|--terminate|--check [...]", header, cmdLineOptions, footer.toString());
                    break;
                case "c":
                    intent = new CreateIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                case "l":
                    intent = new ListIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                case "t":
                    intent = new TerminateIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                case "ch":
                    intent = new ValidationIntent();
                    validator = new CommandLineValidator(cl, intent);
                    break;
                default:
                    return;
            }
            if (intent != null) {
                if (validator.validate()) {
                    try {
                        intent.execute();
                    } catch (IntentNotConfiguredException e) {
                        log.error(e.getMessage());
                    }
                } else {
                    log.error(ABORT_WITH_NOTHING_STARTED);
                }
            }
        } catch (ParseException pe) {
            log.error("Error while parsing the commandline arguments: {}", pe.getMessage());
            log.error(ABORT_WITH_NOTHING_STARTED);
        }
    }
}

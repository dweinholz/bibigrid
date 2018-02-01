package de.unibi.cebitec.bibigrid.core.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Host configuration for the ansible scripts.
 * <p/>
 * The {@link #toString() toString} method outputs the configuration in ini format
 * ready to use for ansible.
 *
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public class AnsibleHostsConfig {
    private final Configuration config;
    private final List<String> slaveIps;

    public AnsibleHostsConfig(Configuration config) {
        this.config = config;
        slaveIps = new ArrayList<>();
    }

    public void addSlaveIp(String slaveIp) {
        slaveIps.add(slaveIp);
    }

    @Override
    public String toString() {
        StringBuilder hostsConfig = new StringBuilder();
        hostsConfig.append("# The content of this file (ansible_hosts) should be generated by ");
        hostsConfig.append("BiBiGrid during instance initialization.\n");
        hostsConfig.append("# Depending on the used base image the ansible_user must be changed. ");
        hostsConfig.append("The local ip address of each slave must\n");
        hostsConfig.append("# inserted in the slave section\n\n");
        hostsConfig.append("[master]\n");
        hostsConfig.append("localhost ansible_connection=local\n\n");
        hostsConfig.append("[slaves]\n");
        for (String slaveIp : slaveIps) {
            hostsConfig.append(slaveIp).append(" ansible_connection=ssh ansible_user=");
            hostsConfig.append(config.getSshUser()).append("\n");
        }
        return hostsConfig.toString();
    }
}

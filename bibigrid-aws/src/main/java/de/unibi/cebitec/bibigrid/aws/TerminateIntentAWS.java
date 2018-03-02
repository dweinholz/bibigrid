package de.unibi.cebitec.bibigrid.aws;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.DeletePlacementGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSecurityGroupRequest;
import com.amazonaws.services.ec2.model.DeleteSubnetRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesRequest;
import com.amazonaws.services.ec2.model.DescribeInstancesResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Reservation;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import de.unibi.cebitec.bibigrid.core.intents.TerminateIntent;
import de.unibi.cebitec.bibigrid.core.model.Cluster;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static de.unibi.cebitec.bibigrid.core.util.VerboseOutputFilter.V;

import java.util.List;

/**
 * AWS specific implementation of terminate intent.
 *
 * @author Johannes Steiner - jsteiner(at)cebitec.uni-bielefeld.de
 * @author Jan Krueger - jkrueger(at)cebitec.uni-bielefeld.de
 */
public class TerminateIntentAWS extends TerminateIntent {
    private static final Logger LOG = LoggerFactory.getLogger(TerminateIntentAWS.class);
    private final ConfigurationAWS config;

    TerminateIntentAWS(ProviderModule providerModule, ConfigurationAWS config) {
        super(providerModule, config);
        this.config = config;
    }

    @Override
    protected boolean terminateCluster(Cluster cluster) {
        final AmazonEC2 ec2 = IntentUtils.getClient(config);
        terminateInstances(ec2, cluster);
        terminatePlacementGroup(ec2, cluster);
        terminateSubnet(ec2, cluster);
        terminateSecurityGroup(ec2, cluster);
        return true;
    }

    private void terminateInstances(final AmazonEC2 ec2, final Cluster cluster) {
        List<String> instances = cluster.getSlaveInstances();
        if (cluster.getMasterInstance() != null) {
            instances.add(cluster.getMasterInstance());
        }
        if (instances.size() > 0) {
            TerminateInstancesRequest terminateInstanceRequest = new TerminateInstancesRequest();
            terminateInstanceRequest.setInstanceIds(instances);
            ec2.terminateInstances(terminateInstanceRequest);
            LOG.info("Wait for instances to shut down. This can take a while, so please be patient!");
            do {
                DescribeInstancesRequest describeInstancesRequest = new DescribeInstancesRequest();
                describeInstancesRequest.setInstanceIds(instances);
                DescribeInstancesResult describeInstancesResult = ec2.describeInstances(describeInstancesRequest);
                boolean allTerminated = true;
                for (Reservation reservation : describeInstancesResult.getReservations()) {
                    for (Instance instance : reservation.getInstances()) {
                        LOG.info(V, "Instance {} {}", instance.getInstanceId(), instance.getState().getName());
                        allTerminated &= instance.getState().getName().equals("terminated");
                    }
                }
                if (allTerminated) {
                    break;
                }
                // wait until instances are shut down
                sleep(5);
            } while (true);
            // log.info("Instance(s) ({}) terminated.",join(",", instances));
        }
    }

    private void terminatePlacementGroup(final AmazonEC2 ec2, final Cluster cluster) {
        if (cluster.getPlacementGroup() != null) {
            DeletePlacementGroupRequest deletePlacementGroupRequest = new DeletePlacementGroupRequest();
            deletePlacementGroupRequest.setGroupName(cluster.getPlacementGroup());
            ec2.deletePlacementGroup(deletePlacementGroupRequest);
            LOG.info("PlacementGroup terminated.");
        }
    }

    private void terminateSubnet(final AmazonEC2 ec2, final Cluster cluster) {
        if (cluster.getSubnet() != null) {
            DeleteSubnetRequest deleteSubnetRequest = new DeleteSubnetRequest();
            deleteSubnetRequest.setSubnetId(cluster.getSubnet());
            ec2.deleteSubnet(deleteSubnetRequest);
            LOG.info("Subnet terminated.");
        }
    }

    private void terminateSecurityGroup(final AmazonEC2 ec2, final Cluster cluster) {
        if (cluster.getSecurityGroup() != null) {
            DeleteSecurityGroupRequest deleteSecurityGroupRequest = new DeleteSecurityGroupRequest();
            deleteSecurityGroupRequest.setGroupId(cluster.getSecurityGroup());
            ec2.deleteSecurityGroup(deleteSecurityGroupRequest);
            LOG.info("Security group terminated.");
        }
    }
}

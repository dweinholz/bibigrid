package de.unibi.cebitec.bibigrid.googlecloud;

import de.unibi.cebitec.bibigrid.core.CommandLineValidator;
import de.unibi.cebitec.bibigrid.core.model.IntentMode;
import de.unibi.cebitec.bibigrid.core.model.ProviderModule;
import de.unibi.cebitec.bibigrid.core.model.exceptions.ConfigurationException;
import de.unibi.cebitec.bibigrid.core.util.DefaultPropertiesFile;
import org.apache.commons.cli.CommandLine;

import java.util.ArrayList;
import java.util.List;

/**
 * @author mfriedrichs(at)techfak.uni-bielefeld.de
 */
public final class CommandLineValidatorGoogleCloud extends CommandLineValidator {
    private final ConfigurationGoogleCloud googleCloudConfig;

    CommandLineValidatorGoogleCloud(final CommandLine cl, final DefaultPropertiesFile defaultPropertiesFile,
                                    final IntentMode intentMode, final ProviderModule providerModule)
            throws ConfigurationException {
        super(cl, defaultPropertiesFile, intentMode, providerModule);
        googleCloudConfig = (ConfigurationGoogleCloud) config;
    }

    @Override
    protected Class<ConfigurationGoogleCloud> getProviderConfigurationClass() {
        return ConfigurationGoogleCloud.class;
    }

    @Override
    protected List<String> getRequiredOptions() {
        List<String> options = new ArrayList<>();
        switch (intentMode) {
            default:
                return null;
            case LIST:
                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID.getShortParam());
                break;
            case TERMINATE:
                options.add(IntentMode.TERMINATE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam());
                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID.getShortParam());
                break;
            case PREPARE:
            case CREATE:
                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
                options.add(RuleBuilder.RuleNames.USE_MASTER_AS_COMPUTE.getShortParam());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_COUNT.getShortParam());
            case VALIDATE:
                options.add(RuleBuilder.RuleNames.MASTER_INSTANCE_TYPE.getShortParam());
                options.add(RuleBuilder.RuleNames.MASTER_IMAGE.getShortParam());
                options.add(RuleBuilder.RuleNames.SLAVE_INSTANCE_TYPE.getShortParam());
                options.add(RuleBuilder.RuleNames.SLAVE_IMAGE.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.REGION.getShortParam());
                options.add(RuleBuilder.RuleNames.AVAILABILITY_ZONE.getShortParam());
                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.GOOGLE_IMAGE_PROJECT_ID.getShortParam());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID.getShortParam());
                break;
            case CLOUD9:
                options.add(IntentMode.CLOUD9.getShortParam());
                options.add(RuleBuilder.RuleNames.CREDENTIALS_FILE.getShortParam());
                options.add(RuleBuilder.RuleNames.GOOGLE_PROJECT_ID.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_USER.getShortParam());
                options.add(RuleBuilder.RuleNames.SSH_PRIVATE_KEY_FILE.getShortParam());
                break;
        }
        return options;
    }

    @Override
    protected boolean validateProviderParameters() {
        return parseGoogleProjectIdParameter() && parseGoogleImageProjectIdParameter();
    }

    private boolean parseGoogleProjectIdParameter() {
        final String shortParam = RuleBuilder.RuleNames.GOOGLE_PROJECT_ID.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                googleCloudConfig.setGoogleProjectId(value);
            }
        }
        return checkRequiredParameter(shortParam, googleCloudConfig.getGoogleProjectId());
    }

    private boolean parseGoogleImageProjectIdParameter() {
        final String shortParam = RuleBuilder.RuleNames.GOOGLE_IMAGE_PROJECT_ID.getShortParam();
        // Parse command line parameter
        if (cl.hasOption(shortParam)) {
            final String value = cl.getOptionValue(shortParam);
            if (!isStringNullOrEmpty(value)) {
                googleCloudConfig.setGoogleImageProjectId(value);
            }
        }
        return checkRequiredParameter(shortParam, googleCloudConfig.getGoogleImageProjectId());
    }
}

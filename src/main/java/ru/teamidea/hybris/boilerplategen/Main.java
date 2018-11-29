package ru.teamidea.hybris.boilerplategen;

import com.squareup.javapoet.JavaFile;
import org.apache.commons.cli.*;
import org.apache.log4j.Logger;
import ru.teamidea.hybris.boilerplategen.core.*;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFileData;
import ru.teamidea.hybris.boilerplategen.core.enums.LayerEnum;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.*;

/**
 * Created by Timofey Klyubin on 14.03.18
 */
public final class Main {

    private static final Logger LOG = Logger.getLogger(Main.class);

    private static final String PLATFORM_OPT_LONG = "platform";
    private static final String MODEL_OPT_LONG = "model";
    private static final String SEARCH_BY_OPT_LONG = "search-by";
    private static final String LAYER_GEN_OPT_LOG = "gen";
    private static final String CMD_LINE_SYNTAX = "boilerplate-generator -m <Type name> -p <Platform Home> " +
                                                          "[-s <search by field(s)>]";

    public static void main(String[] args) throws ParseException {
        Options options = new Options();
        options.addOption("m", MODEL_OPT_LONG, true, "Item type which will be used for code generation. For example: \"MyType\"");
        options.addOption("p", PLATFORM_OPT_LONG, true, "Path to the platform directory of Hybris installation.");
        options.addOption("g", LAYER_GEN_OPT_LOG, true, "Specifies which layer is to be generated. Possible values: dao, service, facade.");
        options.addOption("s", SEARCH_BY_OPT_LONG, true, "Field(s) of the type which will be used to generate search methods. You can " +
                                                          "specify multiple fields concatenated with plus sign (+) " +
                                                          "to generate method which searches using both fields. Example: " +
                                                          "\"code,code+name\".");
        options.addOption("a", "with-find-all", false, "If this option is present, method that retrieves all existing rows " +
                                                               "will be generated.");
        options.addOption("r", "spring-prefix", true, "Prefix which will be appended to generated bean ids.");
        options.addOption("x", "prefix", true, "Prefix which will be appended to generated classes.");

        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine command = parser.parse(options, args);
            if (command.getOptions().length == 0) {
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }

            if (command.getOptionValue(MODEL_OPT_LONG).isEmpty()) {
                LOG.error("Model type name must be specified!");
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }
            // TODO: check options

            File platformPath = new File(command.getOptionValue(PLATFORM_OPT_LONG).replaceFirst("^~", System.getProperty("user.home")));
            if (!platformPath.exists()) {
                LOG.error(String.format("Could not find platform path: %s", platformPath));
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }

            List<File> foundFiles = new ModelClassFinder().findModelJavaFile(platformPath, command.getOptionValue(MODEL_OPT_LONG));
            if (foundFiles.size() > 1) {
                LOG.error("Found more than one file!");
                LOG.error(foundFiles);
                return;
            } else if (foundFiles.isEmpty()) {
                LOG.error("Model class source code not found.");
                return;
            }
            ModelFileData fileData = new ModelDataParser().parseModelFile(foundFiles.get(0));

            final String generationLayer = command.hasOption(LAYER_GEN_OPT_LOG)
                                                   ? command.getOptionValue(LAYER_GEN_OPT_LOG)
                                                   : "dao";
            if (!AbstractGenerator.layersMap.containsKey(generationLayer)) {
                LOG.error("'" + LAYER_GEN_OPT_LOG + "' option must be one of the following: "
                                           + AbstractGenerator.layersMap.keySet());
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }

            /*
             * key - file path
             * value - content
             */
            final Map<File, String> filesToWrite = new HashMap<>();
            final CoreExtensionFinder coreFinder = new CoreExtensionFinder(platformPath.toPath());
            final List<Path> coreExtPathCandidates = coreFinder.findBaseCustomCorePath();
            Path coreExtPath = null;
            if (coreExtPathCandidates.isEmpty()) {
                LOG.error("Couldn't find custom core extension! Could be an internal error.");
                // TODO: failover to tmp dir
                return;
            }
            if (coreExtPathCandidates.size() == 1) {
                coreExtPath = coreExtPathCandidates.iterator().next();
            } else {
                LOG.warn("Couldn't determine custom core extension name for sure, please " +
                                           "choose from one of the following candidates:");

            }
            final Set<LayerEnum> layersToGenerate = AbstractGenerator.layersMap.get(generationLayer);
            for (LayerEnum layerEnum : layersToGenerate) {
                switch (layerEnum) {
                    case DAO: {
                        // generate dao interface
                        DaoGenerator daoGenerator = new DaoGenerator(platformPath, fileData,
                                Collections.singleton(command.getOptionValue(SEARCH_BY_OPT_LONG)));
                        final JavaFile interfaceSource = daoGenerator.generateDaoInterfaceFile(true);
                        LOG.debug(interfaceSource);
                        final JavaFile implSource = daoGenerator.generateDaoImplementationFile(true);
                        LOG.debug(implSource);

                        String interfacePath = interfaceSource.packageName.replaceAll("\\.", File.pathSeparator);
                        String implPath = implSource.packageName.replaceAll("\\.", File.pathSeparator);

                        // generate dao impl
                    } break;
                    case SERVICE: {

                    } break;
                    case FACADE: {

                    } break;
                }
            }


        } catch (UnrecognizedOptionException e) {
            LOG.error("Unrecognized option: " + e.getOption(), e);
            new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
        }
    }
}

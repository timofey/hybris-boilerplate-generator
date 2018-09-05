package ru.teamidea.hybris.boilerplategen;

import org.apache.commons.cli.*;
import ru.teamidea.hybris.boilerplategen.core.AbstractGenerator;
import ru.teamidea.hybris.boilerplategen.core.DaoGenerator;
import ru.teamidea.hybris.boilerplategen.core.ModelClassFinder;
import ru.teamidea.hybris.boilerplategen.core.ModelDataParser;
import ru.teamidea.hybris.boilerplategen.core.data.ModelFileData;
import ru.teamidea.hybris.boilerplategen.core.enums.LayerEnum;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * Created by Timofey Klyubin on 14.03.18
 */
public final class Main {

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
                System.err.println("Model type name must be specified!");
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }
            // TODO: check options

            File platformPath = new File(command.getOptionValue(PLATFORM_OPT_LONG).replaceFirst("^~", System.getProperty("user.home")));
            if (!platformPath.exists()) {
                System.err.println(String.format("Could not find platform path: %s", platformPath));
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }

            List<File> foundFiles = new ModelClassFinder().findModelJavaFile(platformPath, command.getOptionValue(MODEL_OPT_LONG));
            if (foundFiles.size() > 1) {
                System.err.println("Found more than one file!");
                System.err.println(foundFiles);
                return;
            } else if (foundFiles.isEmpty()) {
                System.err.println("Model class source code not found.");
                return;
            }
            ModelFileData fileData = new ModelDataParser().parseModelFile(foundFiles.get(0));

            final String generationLayer = command.hasOption(LAYER_GEN_OPT_LOG)
                                                   ? command.getOptionValue(LAYER_GEN_OPT_LOG)
                                                   : "dao";
            if (!AbstractGenerator.layersMap.containsKey(generationLayer)) {
                System.err.println("'" + LAYER_GEN_OPT_LOG + "' option must be one of the following: "
                                           + AbstractGenerator.layersMap.keySet());
                new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
                return;
            }

            /*
             * key - file path
             * value - content
             */
            final Map<File, String> filesToWrite = new HashMap<>();

            final Set<LayerEnum> layersToGenerate = AbstractGenerator.layersMap.get(generationLayer);
            for (LayerEnum layerEnum : layersToGenerate) {
                switch (layerEnum) {
                    case DAO: {
                        // generate dao interface
                        DaoGenerator daoGenerator = new DaoGenerator(platformPath, fileData,
                                Collections.singleton(command.getOptionValue(SEARCH_BY_OPT_LONG)));
                        System.out.println(daoGenerator.generateDaoInterfaceFile(true));
                        System.out.println(daoGenerator.generateDaoImplementationFile(true));

                        // generate dao impl
                    } break;
                    case SERVICE: {

                    } break;
                    case FACADE: {

                    } break;
                }
            }


        } catch (UnrecognizedOptionException e) {
            System.err.println("Unrecognized option: " + e.getOption());
            new HelpFormatter().printHelp(CMD_LINE_SYNTAX, options);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

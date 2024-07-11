package app;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;
import java.util.LongSummaryStatistics;
import java.util.Objects;
import java.util.Optional;
import java.util.Vector;
import java.util.stream.Stream;

import org.checkerframework.checker.signedness.qual.Unsigned;

import com.google.common.io.Files;

import app.ConfigurationTypes.BulkAlgo;
import app.ConfigurationTypes.Extension;
import app.ConfigurationTypes.KeyExchange;
import app.ConfigurationTypes.TlsVersion;
import app.HandshakeStepping.HandshakeType;
import app.HandshakeStepping.StatisticResultHandshakeSegment;
import de.rub.nds.tlsattacker.core.config.Config;
import de.rub.nds.tlsattacker.core.connection.AliasedConnection;
import de.rub.nds.tlsattacker.core.protocol.ProtocolMessage;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateMessage;
import de.rub.nds.tlsattacker.core.protocol.message.CertificateVerifyMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ChangeCipherSpecMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ClientHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ECDHEServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.EncryptedExtensionsMessage;
import de.rub.nds.tlsattacker.core.protocol.message.FinishedMessage;
import de.rub.nds.tlsattacker.core.protocol.message.HelloVerifyRequestMessage;
import de.rub.nds.tlsattacker.core.protocol.message.RSAServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloDoneMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerHelloMessage;
import de.rub.nds.tlsattacker.core.protocol.message.ServerKeyExchangeMessage;
import de.rub.nds.tlsattacker.core.workflow.WorkflowTrace;
import de.rub.nds.tlsattacker.core.workflow.action.MessageActionFactory;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveAction;
import de.rub.nds.tlsattacker.core.workflow.action.ReceiveTillAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendDynamicClientKeyExchangeAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendDynamicServerCertificateAction;
import de.rub.nds.tlsattacker.core.workflow.action.SendDynamicServerKeyExchangeAction;
import de.rub.nds.tlsattacker.transport.ConnectionEndType;

public class TimeMeasurement {
    // perform measurement for one config and one segmented handshake collection
    public static Long[][] startTimeMeasurement(
        int repetition,
        Config config,
        List<WorkflowTrace> segmentedHandshake,
        Boolean shouldDocument,
        int cleanPercentageOutliner
    ) {
        // run repeatedly through handshake segments
        Long[][] durationForHandshakeSegments = new Long[segmentedHandshake.size()][repetition];
            
        int segCnt = 0;
        for (WorkflowTrace partialTrace: segmentedHandshake) {
            for (int i = 0; i < repetition; i++) {
                long timeElapsed = App.startTlsClient(config, partialTrace);
                durationForHandshakeSegments[segCnt][i] = timeElapsed;
            }
            segCnt++;
        }

        // run statistical analysis
        // for the data set of each handshake segment (always measured from 0.00ms to end of handshake segment)
        StatisticResult[] analysisListHandshake = new StatisticResult[segmentedHandshake.size()];
        segCnt = 0;
        for (Long[] dataSetOneHandshakeSegment: durationForHandshakeSegments) {
            analysisListHandshake[segCnt] = StatisticResult.runStatisticAnalysis(dataSetOneHandshakeSegment);
            segCnt++;
        }
        // statistical analysis for each handshake segment on its own (measured from previous to own handshake segment)
        StatisticResultHandshakeSegment[] analysisListSegments = StatisticResultHandshakeSegment.runStatisticAnalysis(analysisListHandshake);

        // tells us how many percentage of the longest durations for each handshake segment should be removed (those values are considered outliners)
        if (cleanPercentageOutliner > 0) {
            // calculate how many values should be removed
            int cntRemovedValues = (int) (cleanPercentageOutliner * repetition / 100);

            Long[][] durationForHandshakeSegmentsClean = new Long[segmentedHandshake.size()][repetition  - cntRemovedValues];
            // sort each data set of the handshake and cut the highest values
            segCnt = 0;
            for (Long[] segment: durationForHandshakeSegments) {
                Arrays.sort(segment);
                List<Long> segmentList = Arrays.asList(segment);
                List<Long> segmentListClean = segmentList.subList(0, segmentList.size() - cntRemovedValues);

                Long[] tempArray = new Long[segmentList.size() - cntRemovedValues];
                tempArray = segmentListClean.toArray(tempArray);
                durationForHandshakeSegmentsClean[segCnt] = tempArray;

                segCnt++;
            }

            // do same analysis steps as for raw data
            StatisticResult[] analysisListHandshakeClean = new StatisticResult[segmentedHandshake.size()];
            segCnt = 0;
            for (Long[] dataSetOneHandshakeSegmenClean: durationForHandshakeSegmentsClean) {
                analysisListHandshakeClean[segCnt] = StatisticResult.runStatisticAnalysis(dataSetOneHandshakeSegmenClean);
                segCnt++;
            }
            // statistical analysis for each handshake segment on its own (measured from previous to own handshake segment)
            StatisticResultHandshakeSegment[] analysisListSegmentsClean = StatisticResultHandshakeSegment.runStatisticAnalysis(analysisListHandshakeClean);

            // log results if wished
            if (shouldDocument == true) {
                logRawAndCleanMeasurement(config, segmentedHandshake, durationForHandshakeSegments, analysisListHandshake, analysisListSegments, cleanPercentageOutliner, durationForHandshakeSegmentsClean, analysisListHandshakeClean, analysisListSegmentsClean);
            }
        } else {
            // log results if wished
            if (shouldDocument == true) {
                logRawMeasurement(config, segmentedHandshake, durationForHandshakeSegments, analysisListHandshake, analysisListSegments);
            }
        }

        return durationForHandshakeSegments;
    }

    public static class StatisticResult {
        Long min;
        Long max;
        Long mean;
        Long median;
        Long quantil25;
        Long quantil75;
        //Long variance;
        Long standardDeviation;
        Float variationCoefficient;
        //Long confidenceInterval95Min;
        //Long confidenceInterval95Max;
        //Long confidenceInterval99Min;
        //Long confidenceInterval99Max;


        // performs statistical analysis for one data set
        public static StatisticResult runStatisticAnalysis(Long[] dataSet) {
            StatisticResult statisticResult = new StatisticResult();

            // get few statistic values
            //new ArrayList<>(Arrays.asList(array));
            LongSummaryStatistics lss = new ArrayList<>(Arrays.asList(dataSet)).stream().mapToLong((a) -> a).summaryStatistics();
            statisticResult.min = lss.getMin();
            statisticResult.max = lss.getMax();
            statisticResult.mean = (long) lss.getAverage(); // rounding is fine as those numbers are in nano second range while results are only interesting in millisecond area

            // get more advanced statistic values
            // Median, 25 and 75 % percentil (https://studyflix.de/statistik/quantile-1040)
            statisticResult.median = calcQuantil(dataSet, 0.5);
            statisticResult.quantil25 = calcQuantil(dataSet, 0.25);
            statisticResult.quantil75 = calcQuantil(dataSet, 0.75);

            // variance (https://studyflix.de/statistik/empirische-varianz-2016)
            Long tempSum = (long)0;
            for (Long dataPoint: dataSet) {
                tempSum += ((dataPoint - statisticResult.mean) * (dataPoint - statisticResult.mean));
            }
            Long variance = tempSum / (dataSet.length - 1);

            // standard deviation (https://studyflix.de/statistik/standardabweichung-1042)
            // TODO: Problem dass hier Wurzel aus double??
            statisticResult.standardDeviation = (long) Math.sqrt(variance);

            // coefficient of variation (https://studyflix.de/statistik/variationskoeffizient-1043)
            statisticResult.variationCoefficient = (float)statisticResult.standardDeviation / (float)statisticResult.mean;

            return statisticResult;
        }

        // helper function for quantils
        public static Long calcQuantil(Long[] dataSet, Double quantil) {
            //System.out.println("\nQuantil: " + quantil);
            
            //System.out.println("Unsorted Data: " + Arrays.toString(dataSet));
            Arrays.sort(dataSet);
            //System.out.println("Sorted Data: " + Arrays.toString(dataSet));
            
            int countDataPoints = dataSet.length;
            //System.out.println("Length: " + countDataPoints);
    
            if (((countDataPoints * quantil) - (int)(countDataPoints * quantil)) == 0) {
                /*
                System.out.println("if TRUE");
                System.out.println("n*p = " + countDataPoints * quantil);
                System.out.println("runtergerundet n*p = " + (int)(countDataPoints * quantil));
                System.out.println("Selected element: " + (dataSet[(int)(countDataPoints * quantil) - 1] + dataSet[(int)(countDataPoints * quantil)]) / 2);
                */
                // number of data points times quantil is a whole number
                return (dataSet[(int)(countDataPoints * quantil) - 1] + dataSet[(int)(countDataPoints * quantil)]) / 2;
            } else {
                /*
                System.out.println("if FALSE");
                System.out.println("n*p = " + countDataPoints * quantil);
                System.out.println("runtergerundet n*p = " + (int)(countDataPoints * quantil));
                System.out.println("Selected element: " + dataSet[(int)(countDataPoints * quantil)]);
                */
                return dataSet[(int)(countDataPoints * quantil)];
            }
        }

        // creates text overview of statistical analysis
        public static String textualRepresentation(StatisticResult statisticResult) {
            String analysisResultsString = new String();

            analysisResultsString = " Min: " + statisticResult.min/1000000.0 + " ms\n";
            analysisResultsString += " Max: " + statisticResult.max/1000000.0 + " ms\n";
            analysisResultsString += " Average: " + statisticResult.mean/1000000.0 + " ms\n";
            analysisResultsString += " Median: " + statisticResult.median/1000000.0 + " ms\n";
            analysisResultsString += " 25% Quantil: " + statisticResult.quantil25/1000000.0 + " ms\n";
            analysisResultsString += " 75% Quantil: " + statisticResult.quantil75/1000000.0 + " ms\n";
            analysisResultsString += " Std Deviation: " + statisticResult.standardDeviation/1000000.0 + " ms\n";
            analysisResultsString += " Variant Coef: " + String.format("%.3f", statisticResult.variationCoefficient*100.0) + " %\n";

            return analysisResultsString;
        }
    }
    
    // TODO: Add calculation of relevant server steps

    // logs raw data and statistical analysis results into file
    public static void logRawMeasurement(
        Config config,
        List<WorkflowTrace> segmentedHandshake,
        Long[][] durationForHandshakeSegments,
        StatisticResult[] analysisListHandshake,
        StatisticResultHandshakeSegment[] analysisListSegments
    ) {
        try {
            // Get path of the JAR file and strip unnecessary folders
            String jarPath = App.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            String basePath = jarPath.substring(0, jarPath.lastIndexOf("target"));
            
            Date now = Calendar.getInstance().getTime();
            String nowAsString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-mmmm").format(now);
            String logFileName = nowAsString + "_measurement-results";

            File logFile = new File(basePath + "logging/" + logFileName);

            try (PrintWriter out = new PrintWriter(logFile)) {
                out.println("TIME MEASUREMENT RESULTS\n" + nowAsString);
                
                out.println("\n\n#################################");
                out.println("Used Configuration\n");
                out.print(ConfigFactory.getConfigOverview(config));

                out.println("\n\n#################################");
                out.println("Used Handshake Segments\n");
                out.print(segmentedHandshake);

                out.println("\n\n#################################");
                out.println("Used Repititions\n");
                out.print(durationForHandshakeSegments[0].length);
                
                out.println("\n\n#################################");
                out.println("Results: Complete duration to the end of each handshake segment.");
                int segmentCount = 0;
                for (StatisticResult oneResult: analysisListHandshake) {
                    out.println("\nHandshake Segment " + segmentCount);
                    out.print(StatisticResult.textualRepresentation(oneResult));
                    segmentCount++;
                }

                out.println("\n\n#################################");
                out.println("Results: Actual duration for each handshake segment.");
                segmentCount = 0;
                for (StatisticResultHandshakeSegment oneResult: analysisListSegments) {
                    out.println("\nHandshake Segment " + segmentCount);
                    out.print(StatisticResultHandshakeSegment.textualRepresentation(oneResult));
                    segmentCount++;
                }
                
                out.println("\n\n#################################");
                out.println("Detailed Measurement Results");
                out.print(Arrays.deepToString(durationForHandshakeSegments));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    // logs raw data and statistical analysis as well as the cleaned results into file
    public static void logRawAndCleanMeasurement(
        Config config,
        List<WorkflowTrace> segmentedHandshake,
        Long[][] durationForHandshakeSegmentsRaw,
        StatisticResult[] analysisListHandshakeRaw,
        StatisticResultHandshakeSegment[] analysisListSegmentsRaw,
        int removedPercentage,
        Long[][] durationForHandshakeSegmentsClean,
        StatisticResult[] analysisListHandshakeClean,
        StatisticResultHandshakeSegment[] analysisListSegmentsClean
    ) {
        try {
            // Get path of the JAR file and strip unnecessary folders
            String jarPath = App.class
                    .getProtectionDomain()
                    .getCodeSource()
                    .getLocation()
                    .toURI()
                    .getPath();
            String basePath = jarPath.substring(0, jarPath.lastIndexOf("target"));
            
            Date now = Calendar.getInstance().getTime();
            String nowAsString = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-mmmm").format(now);
            String logFileName = nowAsString + "_measurement-results";

            File logFile = new File(basePath + "logging/" + logFileName);

            try (PrintWriter out = new PrintWriter(logFile)) {
                out.println("TIME MEASUREMENT RESULTS\n" + nowAsString);
                
                out.println("\n\n#################################");
                out.println("Used Configuration\n");
                out.print(ConfigFactory.getConfigOverview(config));

                out.println("\n\n#################################");
                out.println("Used Handshake Segments\n");
                out.print(segmentedHandshake);

                out.println("\n\n#################################");
                out.println("Used Repititions\n");
                out.print(durationForHandshakeSegmentsRaw[0].length);
                
                out.println("\n\n##################################################################");
                out.println("\n\nRAW RESULTS");
            
                out.println("\n\n#################################");
                out.println("Raw Results: Complete duration to the end of each handshake segment.");
                int segmentCount = 0;
                for (StatisticResult oneResult: analysisListHandshakeRaw) {
                    out.println("\nHandshake Segment " + segmentCount);
                    out.print(StatisticResult.textualRepresentation(oneResult));
                    segmentCount++;
                }

                out.println("\n\n#################################");
                out.println("Raw Results: Actual duration for each handshake segment.");
                segmentCount = 0;
                for (StatisticResultHandshakeSegment oneResult: analysisListSegmentsRaw) {
                    out.println("\nHandshake Segment " + segmentCount);
                    out.print(StatisticResultHandshakeSegment.textualRepresentation(oneResult));
                    segmentCount++;
                }

                out.println("\n\n##################################################################");
                out.println("\n\nCLEANED RESULTS (removed top " + String.valueOf(removedPercentage) + "% of longest durations)");
            
                out.println("\n\n#################################");
                out.println("Cleaned Results: Complete duration to the end of each handshake segment.");
                segmentCount = 0;
                for (StatisticResult oneResult: analysisListHandshakeClean) {
                    out.println("\nHandshake Segment " + segmentCount);
                    out.print(StatisticResult.textualRepresentation(oneResult));
                    segmentCount++;
                }

                out.println("\n\n#################################");
                out.println("Cleaned Results: Actual duration for each handshake segment.");
                segmentCount = 0;
                for (StatisticResultHandshakeSegment oneResult: analysisListSegmentsClean) {
                    out.println("\nHandshake Segment " + segmentCount);
                    out.print(StatisticResultHandshakeSegment.textualRepresentation(oneResult));
                    segmentCount++;
                }
                
                out.println("\n\n##################################################################");
                out.println("Detailed Measurement Results");

                out.println("\n\n#################################");
                out.println("Raw Detailed Measurement Results");
                out.print(Arrays.deepToString(durationForHandshakeSegmentsRaw));

                out.println("\n\n#################################");
                out.println("Cleaned Detailed Measurement Results");
                out.print(Arrays.deepToString(durationForHandshakeSegmentsClean));

            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }


        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }
}
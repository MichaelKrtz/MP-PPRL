package mp_pprl.incremental_clustering;

import mp_pprl.core.domain.RecordIdentifier;
import mp_pprl.core.encoding.EncodingHandler;
import mp_pprl.core.graph.Edge;
import mp_pprl.core.graph.Cluster;
import mp_pprl.core.graph.WeightedGraph;
import mp_pprl.incremental_clustering.optimization.Hungarian;
import mp_pprl.incremental_clustering.optimization.HungarianAlgorithm;
import mp_pprl.core.Party;

import java.util.*;


public class EarlyMappingClusteringProtocol {
    private final List<Party> parties;
    private final Set<String> unionOfBlocks;
    private final double similarityThreshold;
    private final int minimumSubsetSize;
    private final int bloomFilterLength;

    public EarlyMappingClusteringProtocol(List<Party> parties, Set<String> unionOfBlocks, double similarityThreshold, int minimumSubsetSize, int bloomFilterLength) {
        this.parties = parties;
        this.unionOfBlocks = unionOfBlocks;
        this.similarityThreshold = similarityThreshold;
        this.minimumSubsetSize = minimumSubsetSize;
        this.bloomFilterLength = bloomFilterLength;
    }

    public Set<Cluster> execute(boolean enhancedPrivacy) {
        // Initialization
        WeightedGraph graph = new WeightedGraph();
        Set<Cluster> finalClusters = new HashSet<>();
        // Order parties based on database size
        orderPartiesDesc();
        // Iterate blocks
        System.out.println("Number of blocks: " + unionOfBlocks.size());
        int currentBlock = 0;
        for (String blockKey : unionOfBlocks) {
            System.out.println("Current Block: " + currentBlock);
            currentBlock++;
            WeightedGraph blockGraph = new WeightedGraph();
            for (int i = 0; i < parties.size(); i++) {
                if(enhancedPrivacy) {
                    List<Party> participantParties = getParticipantParties(i);
                    encodeBlockOfParties(participantParties, blockKey);
                }

                if (!parties.get(i).getRecordIdentifierGroups().containsKey(blockKey)) {
                    continue;
                }

                List<RecordIdentifier> block = parties.get(i).getRecordIdentifierGroups().get(blockKey);

                if (blockGraph.getClusters().isEmpty()) {
                    for (RecordIdentifier recordIdentifier : block) {
                        Cluster cluster = new Cluster(recordIdentifier);
                        blockGraph.addCluster(cluster);
                    }
                    continue;
                }

                Set<Cluster> newClusterSet = new HashSet<>();
                for (RecordIdentifier recordIdentifier : block) {
                    Cluster newCluster = new Cluster(recordIdentifier);
                    newClusterSet.add(newCluster);
                    for (Cluster cluster : blockGraph.getClusters()) {
                        double similarity;
                        if (enhancedPrivacy) {
                            similarity = SimilarityCalculator.averageSimilaritySecure(cluster, recordIdentifier, bloomFilterLength);
                        } else {
                            similarity = SimilarityCalculator.averageSimilarity(cluster, recordIdentifier);
                        }

                        if (similarity >= similarityThreshold) {
                            blockGraph.addEdge(cluster, newCluster, similarity);
                        }
                    }
                }
                // Add new records to the block's graph.
                blockGraph.addClusters(newClusterSet);
                // Find optimal edges.
                Set<Edge> optimalEdges = HungarianAlgorithm.computeAssignments(blockGraph.getEdges(), true);
//                Set<Edge> optimalEdges = Hungarian.computeAssignments(blockGraph.getEdges(), true);
                // Prune edges that are not optimal.
                blockGraph.getEdges().removeIf(e -> !optimalEdges.contains(e));
                // Merge clusters.
                blockGraph.mergeClusters();
            }

            graph.addClusters(blockGraph.getClusters());
        }

        for (Cluster cluster : graph.getClusters()) {
            if (cluster.recordIdentifiersSet().size() >= minimumSubsetSize) {
                finalClusters.add(cluster);
            }
        }

        return finalClusters;
    }

    private void encodeParties(List<Party> participantParties) {
        EncodingHandler encodingHandler = new EncodingHandler();
        for (Party party : participantParties) {
            party.encodeRecords(encodingHandler);
        }
    }

    private void encodeBlockOfParties(List<Party> participantParties, String block) {
        EncodingHandler encodingHandler = new EncodingHandler();
        for (Party party : participantParties) {
            party.encodeRecordsOfBlock(encodingHandler, block);
        }
    }

    private List<Party> getParticipantParties(int indexOfCurrentParty) {
        ArrayList<Party> participantParties = new ArrayList<>();
        for (int i = 0; i <= indexOfCurrentParty; i++) {
            participantParties.add(parties.get(i));
        }

        return participantParties;
    }

    private void orderPartiesDesc() {
        Comparator<Party> comp = Comparator.comparingInt(Party::getRecordsSize);
        parties.sort(comp.reversed());
    }

}

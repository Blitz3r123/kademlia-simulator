package peersim.kademlia.das;

import java.math.BigInteger;
import peersim.config.Configuration;
import peersim.core.CommonState;
import peersim.core.Network;
import peersim.core.Node;
import peersim.kademlia.KademliaCommonConfig;
import peersim.kademlia.KademliaNode;
import peersim.kademlia.KademliaProtocol;
import peersim.kademlia.UniformRandomGenerator;

/**
 * This control initializes the whole network (that was already created by peersim) assigning a
 * unique NodeId, randomly generated, to every node.
 *
 * @author Daniele Furlan, Maurizio Bonani
 * @version 1.0
 */
public class CustomDistributionDas implements peersim.core.Control {

  private static final String PAR_PROT_KAD = "protocolkad";
  private static final String PAR_PROT_DAS = "protocoldas";
  private static final String PAR_PROT_EVIL_KAD = "protocolEvilkad";
  private static final String PAR_PROT_EVIL_DAS = "protocolEvildas";
  private static final String PAR_EVIL_RATIO = "evilNodeRatio";

  /** Protocol identifiers for Kademlia, DAS, etc. * */
  private int protocolKadID;

  private int protocolEvilKadID;
  private int protocolDasID;
  private int protocolEvilDasID;
  /** Ratio of evil nodes to total number of nodes * */
  private double evilRatio;

  private BigInteger builderAddress;
  private UniformRandomGenerator urg;

  public CustomDistributionDas(String prefix) {
    protocolKadID = Configuration.getPid(prefix + "." + PAR_PROT_KAD);
    protocolEvilKadID = Configuration.getPid(prefix + "." + PAR_PROT_EVIL_KAD, protocolKadID);
    protocolDasID = Configuration.getPid(prefix + "." + PAR_PROT_DAS);
    protocolEvilDasID = Configuration.getPid(prefix + "." + PAR_PROT_EVIL_DAS);
    evilRatio = Configuration.getDouble(prefix + "." + PAR_EVIL_RATIO, 0.0);
    urg = new UniformRandomGenerator(KademliaCommonConfig.BITS, CommonState.r);
  }

  /**
   * Scan over the nodes in the network and assign a randomly generated NodeId in the space
   * 0..2^BITS, where BITS is a parameter from the kademlia protocol (usually 160)
   *
   * @return boolean always false
   */
  public boolean execute() {
    int numEvilNodes = (int) (Network.size() * evilRatio);
    System.out.println("Number of malicious nodes: " + numEvilNodes);

    for (int i = 0; i < Network.size(); ++i) {
      Node generalNode = Network.get(i);
      BigInteger id;
      KademliaNode node;
      id = urg.generate();
      node = new KademliaNode(id, "0.0.0.0", 0);

      KademliaProtocol kadProt = null;
      DASProtocol dasProt = null;

      /** Generate honest and evil nodes * */
      if ((i > 0) && (i < (numEvilNodes + 1))) {
        kadProt = ((KademliaProtocol) (Network.get(i).getProtocol(protocolEvilKadID)));
        dasProt = ((EvilDASProtocol) (Network.get(i).getProtocol(protocolEvilDasID)));
        kadProt.setProtocolID(protocolEvilKadID);
        node.setEvil(true);
        dasProt.setDASProtocolID(protocolEvilDasID);
      } else {
        kadProt = ((KademliaProtocol) (Network.get(i).getProtocol(protocolKadID)));
        dasProt = ((DASProtocol) (Network.get(i).getProtocol(protocolDasID)));
        dasProt.setDASProtocolID(protocolDasID);
        kadProt.setProtocolID(protocolKadID);
      }

      generalNode.setKademliaProtocol(kadProt);
      generalNode.setDASProtocol(dasProt);
      kadProt.setNode(node);
      dasProt.setKademliaProtocol(kadProt);
      kadProt.setEventsCallback(dasProt);

      if (i == 0) {
        dasProt.setBuilder(true);
        builderAddress = dasProt.getKademliaProtocol().getKademliaNode().getId();
      } else dasProt.setBuilderAddress(builderAddress);
    }

    return false;
  }
}

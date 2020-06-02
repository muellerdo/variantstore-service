package life.qbic.oncostore.model

import life.qbic.oncostore.model.Consequence
import life.qbic.oncostore.model.ReferenceGenome

interface SimpleVariantContext {

    String getChromosome()

    BigInteger getStartPosition()

    BigInteger getEndPosition()

    String getReferenceAllele()

    String getObservedAllele()

    List<Consequence> getConsequences()

    List<Genotype> getGenotypes()

    ReferenceGenome getReferenceGenome()

    String getDatabaseId()

    Boolean getIsSomatic()

    String getId()

    VcfInfo getVcfInfo()

    void setId(String id)

    void setIsSomatic(Boolean somatic)

    void setConsequences(List<Consequence> consequences)
}
package life.qbic.oncostore.service

import groovy.util.logging.Log4j2
import htsjdk.samtools.util.CloseableIterator
import htsjdk.variant.vcf.VCFFileReader
import life.qbic.oncostore.model.*
import life.qbic.oncostore.parser.EnsemblParser
import life.qbic.oncostore.parser.MetadataReader
import life.qbic.oncostore.parser.SimpleVCFReader
import life.qbic.oncostore.util.AnnotationHandler
import life.qbic.oncostore.util.ListingArguments
import life.qbic.oncostore.util.VariantExporter

import javax.inject.Inject
import javax.inject.Singleton
import javax.validation.constraints.NotNull

@Log4j2
@Singleton
class VariantstoreInformationCenter implements VariantstoreService{

    private final VariantstoreStorage storage

    @Inject
    VariantstoreInformationCenter(VariantstoreStorage storage) {
        this.storage = storage
    }

    @Override
    List<Case> getCaseForCaseId(String caseId) {
        return storage.findCaseById(caseId)
    }

    @Override
    List<Variant> getVariantForVariantId(String variantId) {
        return storage.findVariantById(variantId)
    }

    @Override
    List<Gene> getGeneForGeneId(String geneId, @NotNull ListingArguments args) {
        return storage.findGeneById(geneId, args)
    }

    @Override
    BeaconAlleleResponse getBeaconAlleleResponse(String chromosome, BigInteger start,
                                        String reference, String observed, String assemblyId) {

        List<Variant> variants = storage.findVariantsForBeaconResponse(chromosome, start, reference, observed, assemblyId)

        BeaconAlleleRequest request = new BeaconAlleleRequest()
        request.setAlternateBases(observed)
        request.setAssemblyId(assemblyId)
        request.setReferenceBases(reference)
        request.setReferenceName(chromosome)
        request.setStart(start)

        BeaconAlleleResponse response = new BeaconAlleleResponse()
        response.setAlleleRequest(request)
        response.setExists(!variants.empty)

        return response
    }

    @Override
    List<Sample> getSampleForSampleId(String sampleId) {
        return storage.findSampleById(sampleId)
    }

    @Override
    List<Case> getCasesForSpecifiedProperties(@NotNull ListingArguments args) {
        return storage.findCases(args)
    }

    @Override
    List<Sample> getSamplesForSpecifiedProperties(@NotNull ListingArguments args) {
        return storage.findSamples(args)
    }

    @Override
    List<Variant> getVariantsForSpecifiedProperties(@NotNull ListingArguments args) {
        return storage.findVariants(args)
    }

    @Override
    List<Gene> getGenesForSpecifiedProperties(@NotNull ListingArguments args) {
        return storage.findGenes(args)
    }

    @Override
    String getVcfContentForVariants(List<Variant> variants) {
        // order variants by contig and position in order to get valid VCF file
        return VariantExporter.exportVariantsToVCF(variants.sort { it.startPosition })
    }

    /**
     * Stores variants given in VCF file in the store.
     * @param url path to the VCF file
     */
    @Override
    void storeVariantsInStore(String url) {
        MetadataReader meta = new MetadataReader(new File(url))

        def vcfFiles = meta.getMetadataContext().getVcfFiles()
        def variantsToInsert = []

        vcfFiles.each { filePath ->
            SimpleVCFReader reader = new SimpleVCFReader((new VCFFileReader(new File(filePath), false)))
            CloseableIterator variants = reader.iterator()

            variants.each {SimpleVariantContext variant ->
                AnnotationHandler.addAnnotationsToVariant(variant, meta.getMetadataContext().getVariantAnnotation())
                variant.setIsSomatic(meta.getMetadataContext().getIsSomatic())
                variantsToInsert.add(variant)
            }
        }

        log.info("Storing provided metadata and variants in store")
        storage.storeVariantsInStoreWithMetadata(meta.getMetadataContext(), variantsToInsert)
        log.info("...done.")
    }

    /**
     * Stores gene information provided in a GFF3 file in the store.
     * @param url path to the GFF3 file
     */
    @Override
    void storeGeneInformationInStore(String url) {
        EnsemblParser ensembl = new EnsemblParser(url)
        log.info("Storing provided gene information in store")
        storage.storeGenesWithMetadata(ensembl.version, ensembl.date, ensembl.referenceGenome, ensembl.genes)
        log.info("...done.")
    }
}

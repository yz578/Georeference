package georeference;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Scanner;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.spatial.tier.projections.CartesianTierPlotter;
import org.apache.lucene.spatial.tier.projections.IProjector;
import org.apache.lucene.spatial.tier.projections.SinusoidalProjector;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.NumericUtils;
import org.apache.lucene.util.Version;

public class GeoIndexWriter {
	
	public static final String LAT_FIELD = "lat";
	public static final String LNG_FIELD = "lng";
	public static final String TIER_PREFIX_FIELD = "_localTier";
    private IProjector projector = new SinusoidalProjector();
    private CartesianTierPlotter ctp =new CartesianTierPlotter(0, projector, TIER_PREFIX_FIELD);
    public static int START_TIER = 5;
    public static int END_TIER = 15;
    
    private Analyzer analyzer = new StandardAnalyzer(Version.LUCENE_34);
    private String locSplitString = "/";
    private String latLngSplitString = ",";
    private String datSourceSplitString = "\t";
	
	public GeoIndexWriter() {
		
	}
	
	private void addDoc(IndexWriter w, String[] fieldNames, String[] values) throws IOException {
	    Document doc = new Document();
	    for (int i = 0; i < fieldNames.length; i++) {
	    	doc.add(new Field(fieldNames[i], values[i], Field.Store.YES, Field.Index.ANALYZED));
	    }
	    w.addDocument(doc);
	}
	
	private void addSpatialLuceneFields(double lat, double lng, Document doc) {
        doc.add(new Field(LAT_FIELD, NumericUtils.doubleToPrefixCoded(lat), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field(LNG_FIELD, NumericUtils.doubleToPrefixCoded(lng), Field.Store.NO, Field.Index.NOT_ANALYZED));
        for (int tier = START_TIER; tier <= END_TIER; tier++) {
            ctp = new CartesianTierPlotter(tier, projector, TIER_PREFIX_FIELD);
            double boxId = ctp.getTierBoxId(lat, lng);
            doc.add(new Field(ctp.getTierFieldName(), NumericUtils.doubleToPrefixCoded(boxId), Field.Store.YES,
                Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }
	
	private void addGeonameDoc(IndexWriter w, String[] fieldNames, String[] values) throws IOException {
	    Document doc = new Document();
	    doc.add(new Field(fieldNames[1], values[1], Field.Store.YES, Field.Index.ANALYZED));
	    doc.add(new Field(fieldNames[2], values[2], Field.Store.NO, Field.Index.ANALYZED));
	    doc.add(new Field(fieldNames[3], values[3], Field.Store.NO, Field.Index.ANALYZED));
	    
	    double lat = Double.parseDouble(values[4]);
	    NumericField latField = new NumericField(fieldNames[4], Field.Store.YES, true);
	    latField.setDoubleValue(lat);
	    doc.add(latField);
	    
	    double lng = Double.parseDouble(values[5]);
	    NumericField lngField = new NumericField(fieldNames[5], Field.Store.YES, true);
	    lngField.setDoubleValue(lng);
	    doc.add(lngField);
	    
	    addSpatialLuceneFields(lat, lng, doc);
	    
	    w.addDocument(doc);
	}
	
	private void addHistnameDoc(IndexWriter w, String[] fieldNames, String[] values) throws IOException {
	    Document doc = new Document();
	    //doc.add(new Field(fieldNames[4], values[4], Field.Store.YES, Field.Index.NO));
	    doc.add(new Field(fieldNames[8], values[8], Field.Store.NO, Field.Index.ANALYZED));
	    doc.add(new Field(fieldNames[10], values[10], Field.Store.NO, Field.Index.ANALYZED));
	    
	    double lat = Double.parseDouble(values[13]);
	    NumericField latField = new NumericField(fieldNames[13], Field.Store.YES, true);
	    latField.setDoubleValue(lat);
	    doc.add(latField);
	    
	    double lng = Double.parseDouble(values[15]);
	    NumericField lngField = new NumericField(fieldNames[15], Field.Store.YES, true);
	    lngField.setDoubleValue(lng);
	    doc.add(lngField);
	    
	    //doc.add(new Field(fieldNames[18], values[18], Field.Store.YES, Field.Index.NO));
	    doc.add(new Field(fieldNames[19], values[19], Field.Store.YES, Field.Index.ANALYZED));
	    
	    addSpatialLuceneFields(lat, lng, doc);
	    
	    w.addDocument(doc);
	}
	
	public Directory getGeonameDirectory(String dataSourceFilePath, String indexTargetFilePath) throws IOException {
		File file = new File(indexTargetFilePath);
		Directory index = null;		
		if (file.exists()) {
			index = new SimpleFSDirectory(file);
		}
		else {
			index = FSDirectory.open(file);
			
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_34, analyzer);
			IndexWriter indexWriter = new IndexWriter(index, indexWriterConfig);
			
			// Geonames
			String[] geonameFields = {
										"geonameid", "name", "asciiname", "alternatenames", "latitude", "longitude",
										"feature class", "feature code", "country code", "cc2", "admin1 code",
										"admin2 code", "admin3 code", "admin4 code", "population", "elevation",
										"gtopo30", "timezone", "modification date"
										};
			
			FileInputStream fstream = new FileInputStream(dataSourceFilePath);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String strLine;
			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   {
				String[] splittedLine = strLine.split(datSourceSplitString);
				if (splittedLine.length >= geonameFields.length) {
					addGeonameDoc(indexWriter, geonameFields, splittedLine);
				}
			}
			indexWriter.close();
		}
		
		return index;
	}
	
	public Directory getHistnameDirectory(String dataSourceFilePath, String indexTargetFilePath) throws IOException {
		File file = new File(indexTargetFilePath);
		Directory index = null;		
		if (file.exists()) {
			index = new SimpleFSDirectory(file);
		}
		else {
			index = FSDirectory.open(file);
			
			IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_34, analyzer);
			IndexWriter indexWriter = new IndexWriter(index, indexWriterConfig);
			
			FileInputStream fstream = new FileInputStream(dataSourceFilePath);
			// Get the object of DataInputStream
			DataInputStream in = new DataInputStream(fstream);
			BufferedReader br = new BufferedReader(new InputStreamReader(in));
			String fieldLine = br.readLine();
			String[] histnameFields = fieldLine.split(datSourceSplitString);
			String strLine;
			//Read File Line By Line
			while ((strLine = br.readLine()) != null)   {
				String[] splittedLine = strLine.split(datSourceSplitString);
				if (splittedLine.length >= histnameFields.length) {
					addHistnameDoc(indexWriter, histnameFields, splittedLine);
				}
			}
			
			indexWriter.close();
		}
		
		return index;
	}
}

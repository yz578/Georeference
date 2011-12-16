package edu.cornell.georeference;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.NumericField;
import org.apache.lucene.index.CorruptIndexException;
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

/**
 * 
 * This class helps creates indices/directories out of raw data source files
 * 
 * @author Yang Yang Zheng
 *
 */
@SuppressWarnings("deprecation")
public class GeoIndexWriter {
	
	// Names of the fields in the document
	public static final String NAME_FIELD = "name";
	public static final String ALT_NAME1_FIELD = "altname1";
	public static final String ALT_NAME2_FIELD = "altname2";
	public static final String LATITUDE_FIELD = "latitude";
	public static final String LONGITUDE_FIELD = "longitude";
	
	// Names of the fields used for the Lucene Spatial
	public static final String SPT_LAT_FIELD = "lat";
	public static final String SPT_LNG_FIELD = "lng";
	public static final String TIER_PREFIX_FIELD = "_localTier";
	
    // Tier values for Lucene Spatial
    public static final int START_TIER = 5;
    public static final int END_TIER = 15;
    
    private IProjector projector;
    private CartesianTierPlotter ctp;
    private Analyzer analyzer;
	
    /**
     * Create an object and initializes the default analyzer, projector, etc
     */
	public GeoIndexWriter() {
		this.projector = new SinusoidalProjector();
	    this.ctp = new CartesianTierPlotter(0, projector, TIER_PREFIX_FIELD);
	    this.analyzer = new StandardAnalyzer(Version.LUCENE_34);
	}
	
	/**
	 * Check to see if a file exists
	 * 
	 * @param filePath  the path to the file
	 * @return whether or not the file exists
	 */
	public boolean hasFileAtPath(String filePath) {
		File file = new File(filePath);
		return file.exists();
	}
	
	/**
	 * Get the index from the file path
	 * 
	 * @param indexFilePath  the file path of the index
	 * @return the index/directory
	 * @throws IOException
	 */
	public Directory getIndexFromFilePath(String indexFilePath) throws IOException {
		File file = new File(indexFilePath);
		Directory index = null;		
		if (file.exists()) {
			index = new SimpleFSDirectory(file);
		}
		return index;
	}
	
	/**
	 * Delete a file
	 * 
	 * @param file  the file that is to be deleted
	 * @return whether or not the deletion is successful
	 */
	public static boolean deleteFile(File file) {
		// if the file is a directory, delete all files in it before delete itself
	    if (file.isDirectory()) {
	        for (File subFile : file.listFiles()) {
	            deleteFile(subFile);
	        }
	    }
	    return file.delete();
	}
	
	/**
	 * Build an index/directory if there isn't one at the index file path
	 * 
	 * @param dataSource  the data source that is used to build the index
	 * @param indexTargetFilePath  the file directory to put the index in
	 * @return the index/directory at the index file directory
	 * @throws IOException
	 */
	public Directory buildIndexIfIndexNotExist(GeoDataSource dataSource, String indexTargetFilePath) throws IOException {
		// Check to see if there is an index at the index file path
		Directory index = getIndexFromFilePath(indexTargetFilePath);
		if (index != null) {
			return index;
		}
		else {
			return buildIndex(dataSource, indexTargetFilePath);
		}
	}
	
	/**
	 * Build an index/directory if there isn't one at the index file path. It will delete the current
	 * existing file at the index file path
	 * 
	 * @param dataSource  the data source that is used to build the index
	 * @param indexTargetFilePath  the file directory to put the index in
	 * @return the index/directory built
	 * @throws IOException
	 */
	public Directory buildIndex(GeoDataSource dataSource, String indexTargetFilePath) throws IOException {
		// Check to see if there is already an file at the index file path
		// Delete the file if there is one
		File file = new File(indexTargetFilePath);
		if (file.exists()) {
			// return null if it can't delete the file
			if (!deleteFile(file)) {
				return null;
			}
		}
		
		file = new File(indexTargetFilePath);
		Directory index = FSDirectory.open(file);
		
		IndexWriterConfig indexWriterConfig = new IndexWriterConfig(Version.LUCENE_34, analyzer);
		IndexWriter indexWriter = new IndexWriter(index, indexWriterConfig);
		
		FileInputStream fstream = new FileInputStream(dataSource.getFilePath());
		
		// Get the object of DataInputStream
		DataInputStream in = new DataInputStream(fstream);
		BufferedReader br = new BufferedReader(new InputStreamReader(in));
		
		// Whether to not to ingore first row (some data source file has column names as the first row)
		if (dataSource.isIgnoreFirstRow()) {
			br.readLine();
		}
		
		//Read File Line By Line
		String strLine;
		while ((strLine = br.readLine()) != null)   {
			String[] splittedLine = strLine.split(dataSource.getDatSourceSplitString());
			if (splittedLine.length >= dataSource.getNumOfCols()) {
				addDoc(dataSource, indexWriter, splittedLine);
			}
		}
					
		indexWriter.close();
				
		return index;
	}
	
	// All methods below are private helper methods that are used to help build the indices
	
	// Creating fields needed for the Lucene Spatial
	private void addSpatialLuceneFields(double lat, double lng, Document doc) {
        doc.add(new Field(SPT_LAT_FIELD, NumericUtils.doubleToPrefixCoded(lat), Field.Store.NO, Field.Index.NOT_ANALYZED));
        doc.add(new Field(SPT_LNG_FIELD, NumericUtils.doubleToPrefixCoded(lng), Field.Store.NO, Field.Index.NOT_ANALYZED));
        for (int tier = START_TIER; tier <= END_TIER; tier++) {
            ctp = new CartesianTierPlotter(tier, projector, TIER_PREFIX_FIELD);
            double boxId = ctp.getTierBoxId(lat, lng);
            doc.add(new Field(ctp.getTierFieldName(), NumericUtils.doubleToPrefixCoded(boxId), Field.Store.YES,
                Field.Index.NOT_ANALYZED_NO_NORMS));
        }
    }
	
	// Create a document for the place and add all relevant fields to the documents
	private void addDoc(GeoDataSource dataSource, IndexWriter w, String[] values) throws CorruptIndexException, IOException {
		 Document doc = new Document();
		 
		 // Add the name field to the document
		 Field mainNameField = new Field(NAME_FIELD, values[dataSource.getNameColNum()], Field.Store.YES, 
				 Field.Index.ANALYZED);
		 mainNameField.setBoost(1.2f); // Add boost to this field to boost the score if match
		 doc.add(mainNameField);
		 
		 // Add other alternative name fields;
		 doc.add(new Field(ALT_NAME1_FIELD, values[dataSource.getAltName1ColNum()], Field.Store.NO, 
				 Field.Index.ANALYZED));
		 doc.add(new Field(ALT_NAME2_FIELD, values[dataSource.getAltName2ColNum()], Field.Store.NO, 
				 Field.Index.ANALYZED));
		    
		 // Add latitude field to the document
		 double lat = Double.parseDouble(values[dataSource.getLatColNum()]);
		 NumericField latField = new NumericField(LATITUDE_FIELD, Field.Store.YES, true);
		 latField.setDoubleValue(lat);
		 doc.add(latField);
		    
		// Add longitude field to the document
		 double lng = Double.parseDouble(values[dataSource.getLngColNum()]);
		 NumericField lngField = new NumericField(LONGITUDE_FIELD, Field.Store.YES, true);
		 lngField.setDoubleValue(lng);
		 doc.add(lngField);
		    
		 addSpatialLuceneFields(lat, lng, doc);
		    
		 w.addDocument(doc);
	}
}

package edu.cornell.georeference;

import java.util.Map;

/**
 * This class represents the data source. It stores all information needed to build the index
 * from the data source.
 * 
 * @author Yang Yang Zheng
 *
 */
public class GeoDataSource {
	
	private String filePath;
	private String datSourceSplitString;
	private int numOfCols;
	private int nameColNum;
	private int altName1ColNum;
	private int altName2ColNum;
	private int latColNum;
	private int lngColNum;
	private boolean ignoreFirstRow;
	
	// Constructors
	
	/**
	 * Create the data source using the file path; need to fill in values for other fields later
	 * 
	 * @param filePath  the path to the data source file
	 */
	public GeoDataSource(String filePath) {
		this.filePath = filePath;
		this.datSourceSplitString = "\t";
	}
	
	/**
	 * Create the data source using the file path and other details.
	 * 
	 * @param filePath  the path to the data source file
	 * @param datSourceSplitString  what string is used to separate different fields in the data file
	 * @param numOfCols  number of columns in the data file
	 * @param nameColNum  the column number for the name column
	 * @param altName1ColNum  the column number of the first alternative name column
	 * @param altName2ColNum  the column number of the second alternative name column
	 * @param latColNum  the column number of the latitude column
	 * @param lngColNum  the column number of the longitude column
	 * @param ignoreFirstRow  whether or not to ignore the first row when reading in the file (some
	 * files have the column names as the first row)
	 */
	public GeoDataSource(String filePath, String datSourceSplitString, int numOfCols, int nameColNum, 
			int altName1ColNum, int altName2ColNum, int latColNum, int lngColNum, boolean ignoreFirstRow) {
		this.filePath = filePath;
		this.datSourceSplitString = datSourceSplitString;
		this.numOfCols = numOfCols;
		setColNums(nameColNum, altName1ColNum, altName2ColNum, latColNum, lngColNum);
		this.ignoreFirstRow = ignoreFirstRow;
	}
	
	/**
	 * Create the data source using the file path and other details.
	 * 
	 * @param filePath the path to the data source file
	 * @param datSourceSplitString  what string is used to separate different fields in the data file
	 * @param numOfCols  number of columns in the data file
	 * @param colNumMap  a map that maps field name to the column number (must use the field names in
	 * the GeoIndexWriter class)
	 * @param ignoreFirstRow  whether or not to ignore the first row when reading in the file (some
	 * files have the column names as the first row)
	 */
	public GeoDataSource(String filePath, String datSourceSplitString, int numOfCols, 
			Map<String, Integer> colNumMap, boolean ignoreFirstRow) {
		this.filePath = filePath;
		this.datSourceSplitString = datSourceSplitString;
		this.numOfCols = numOfCols;
		setColNums(colNumMap);
		this.ignoreFirstRow = ignoreFirstRow;
	}
	
	/**
	 * Creates a GeoDataSource object from the data that come from this website:
	 * http://www.geonames.org/
	 * 
	 * @param filePath  the path to the data file
	 * @return the GeoDataSource object that represent the data source and can be used by
	 * GeoIndexWriter to create index/directory
	 */
	public static GeoDataSource createGeoNamesDataSource(String filePath) {
		return new GeoDataSource(filePath, "\t", 19, 1, 2, 3, 4, 5, false);
	}
	/**
	 * Creates a GeoDataSource object from the data that come from this website:
	 * http://thedatahub.org/dataset/pleiades
	 * The file needs to be exported from the spreadsheet as a tab delimited file
	 * 
	 * @param filePath  the path to the data file
	 * @return the GeoDataSource object that represent the data source and can be used by
	 * GeoIndexWriter to create index/directory
	 */
	public static GeoDataSource createPleiadesDataSource(String filePath) {
		return new GeoDataSource(filePath, "\t", 21, 19, 8, 10, 13, 15, true);
	}
	
	// Getters and setters
	
	/**
	 * Set the column number for all fields.
	 * 
	 * @param nameColNum  the column number for the name column
	 * @param altName1ColNum  the column number of the first alternative name column
	 * @param altName2ColNum  the column number of the second alternative name column
	 * @param latColNum  the column number of the latitude column
	 * @param lngColNum  the column number of the longitude column
	 */
	public void setColNums(int nameColNum, int altName1ColNum, int altName2ColNum, 
			int latColNum, int lngColNum) {
		this.nameColNum = nameColNum;
		this.altName1ColNum = altName1ColNum;
		this.altName2ColNum = altName2ColNum;
		this.latColNum = latColNum;
		this.lngColNum = lngColNum;
	}
	
	/**
	 * Set the column number for all fields using a map.
	 * 
	 * @param colNumMap  a map that maps field name to the column number (must use the field names in
	 * the GeoIndexWriter class)
	 */
	public void setColNums(Map<String, Integer> colNumMap) {
		nameColNum = colNumMap.get(GeoIndexWriter.NAME_FIELD);
		altName1ColNum = colNumMap.get(GeoIndexWriter.ALT_NAME1_FIELD);
		altName2ColNum = colNumMap.get(GeoIndexWriter.ALT_NAME2_FIELD);
		latColNum = colNumMap.get(GeoIndexWriter.LATITUDE_FIELD);
		lngColNum = colNumMap.get(GeoIndexWriter.LONGITUDE_FIELD);
	}
	
	public String getFilePath() {
		return filePath;
	}

	public void setFilePath(String filePath) {
		this.filePath = filePath;
	}
	
	public String getDatSourceSplitString() {
		return datSourceSplitString;
	}

	public void setDatSourceSplitString(String datSourceSplitString) {
		this.datSourceSplitString = datSourceSplitString;
	}
	
	public int getNumOfCols() {
		return numOfCols;
	}

	public void setNumOfCols(int numOfCols) {
		this.numOfCols = numOfCols;
	}

	public int getNameColNum() {
		return nameColNum;
	}

	public void setNameColNum(int nameColNum) {
		this.nameColNum = nameColNum;
	}

	public int getAltName1ColNum() {
		return altName1ColNum;
	}

	public void setAltName1ColNum(int altName1ColNum) {
		this.altName1ColNum = altName1ColNum;
	}

	public int getAltName2ColNum() {
		return altName2ColNum;
	}

	public void setAltName2ColNum(int altName2ColNum) {
		this.altName2ColNum = altName2ColNum;
	}

	public int getLatColNum() {
		return latColNum;
	}

	public void setLatColNum(int latColNum) {
		this.latColNum = latColNum;
	}

	public int getLngColNum() {
		return lngColNum;
	}

	public void setLngColNum(int lngColNum) {
		this.lngColNum = lngColNum;
	}
	
	public boolean isIgnoreFirstRow() {
		return ignoreFirstRow;
	}

	public void setIgnoreFirstRow(boolean ignoreFirstRow) {
		this.ignoreFirstRow = ignoreFirstRow;
	}
}

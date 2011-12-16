package edu.cornell.georeference;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.function.CustomScoreProvider;
import org.apache.lucene.search.function.CustomScoreQuery;
import org.apache.lucene.search.function.ValueSourceQuery;
import org.apache.lucene.spatial.tier.DistanceQueryBuilder;

/**
 * This class is a child class of the CustomeScoreQuery. It allow the distance to affect the score.
 * 
 * @author Yang Yang Zheng
 *
 */
@SuppressWarnings("serial")
public class DistanceScoreQuery extends CustomScoreQuery {
	
	// Distance score options
	public static final int LINEAR = 1;
	public static final int SQUARE_ROOT = 1;
	public static final int LOGARITHMIC = 1;
	
	private DistanceQueryBuilder dq;
	private int scoreOption;
	
	/**
	 * Create a distance score query by providing the subquery, the distance query builder, and 
	 * the score option
	 * 
	 * @param subQuery  the sub query that is to be wrapped around
	 * @param dq  the distance query builder that provides the distances
	 * @param scoreOption controls how the distance affects the score
	 */
	public DistanceScoreQuery(Query subQuery, DistanceQueryBuilder dq, int scoreOption) {
		super(subQuery);
		this.dq = dq;
		this.scoreOption = scoreOption;
	}
	
	/**
	 * This is the constructor of the class
	 * 
	 * @param subQuery  the sub query that is to be wrapped around
	 * @param valSrcQuery  the query whose scores are used in the custom score computation
	 * @param dq  the distance query builder that provides the distances
	 * @param scoreOption  controls how the distance affects the score
	 */
	public DistanceScoreQuery(Query subQuery, ValueSourceQuery valSrcQuery, DistanceQueryBuilder dq, int scoreOption) {
		super(subQuery, valSrcQuery);
		this.dq = dq;
		this.scoreOption = scoreOption;
	}
	
	// Override
	protected CustomScoreProvider getCustomScoreProvider(IndexReader reader) { 
		return new DistanceScoreProvider(reader, dq, scoreOption);
	}
	
}

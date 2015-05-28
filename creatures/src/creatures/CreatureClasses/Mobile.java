package creatures.CreatureClasses;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import communication.knowledge.Information;
import communication.knowledge.InformationType;
import communication.knowledge.KnowledgeBase;
import communication.messages.AskForFoodMessage;
import communication.messages.AskForHelpMessage;
import communication.messages.QueryMessage;
import creatures.Agent;
import creatures.Fightable;
import map.Food;
import Constants.Constants;
import Enemies.Enemy;
import repast.simphony.context.Context;
import repast.simphony.engine.environment.RunEnvironment;
import repast.simphony.parameter.Parameter;
import repast.simphony.query.space.grid.GridCell;
import repast.simphony.query.space.grid.GridCellNgh;
import repast.simphony.random.RandomHelper;
import repast.simphony.space.SpatialMath;
import repast.simphony.space.continuous.ContinuousSpace;
import repast.simphony.space.continuous.NdPoint;
import repast.simphony.space.grid.Grid;
import repast.simphony.space.grid.GridDimensions;
import repast.simphony.space.grid.GridPoint;
import repast.simphony.util.ContextUtils;

/**
 * @author Asmodiel
 *	base class for mobile
 */
public abstract class Mobile extends Fightable {
	
	public enum GoingWhere
	{
		Uknown,
		Home,
		ForFood,
		HomeWithFood,
		Explore,
		Wpierdol
	}
	
	// creature properties
	private float strength = 0;
	private float size = Constants.CREATURES_SIZE;
	private int food = Constants.MOBILE_STARTING_FOOD;
	
	// formation stuff
	protected boolean isInFormation = false;
	
	
	// carrying stuff
	private int carryCapacity = Constants.MOBILE_CARRY_CAPACITY;
	private Food carriedStuff;
	
	// stats
	private int experience = 0;
	private int intelligence = 0;
	private int diplomacySkill = 0;
	private int agression = 0;
	private boolean isStarving = false;
	
	//Moving logic
	private boolean isGoingSomewhere = false;
	private GridPoint goingPoint;
	private GoingWhere goingWhere = GoingWhere.Uknown;
	private boolean move = true;

	private List<Mobile> bros = new ArrayList<Mobile>();
	
	private KnowledgeBase knowledgeBase = new KnowledgeBase(Constants.MOBILE_MAX_KNOWLEDGE);
	
	public Mobile( ContinuousSpace < Object > space, Grid< Object > grid, int setPlayerID)
	{
		super(space, grid, setPlayerID, Constants.MOBILE_ATTACK, Constants.MOBILE_HEALTH, Constants.MOBILE_MEAT_NO);
	}


	// are we standing on food?
	public List<Food> FoodAtPoint(GridPoint pt)
	{
		List<Food> food = new ArrayList<Food>();
		for (Object obj : grid.getObjectsAt(pt.getX(), pt.getY())) {
			if (obj instanceof Food) {
				food.add( (Food) obj);
			}
		}
		return food;
	}
	
	private void StartCarrying( Food food )
	{
		this.carriedStuff = food;
		food.setPicked(true);
	}
	
	protected void MoveCarriedStuff()
	{
		if ( carriedStuff != null )
		{
			// get current location in grid
			GridPoint gp = grid.getLocation(this);
			space.moveTo(carriedStuff, gp.getX(), gp.getY());
			grid.moveTo(carriedStuff, gp.getX(), gp.getY());
		}
	}
	
	public void MoveThere()
	{
		moveTowards( goingPoint );
	}
	
	public boolean IsAtDestination()
	{
		if ( goingPoint == null ) return false;
		
		// get current location in grid
		GridPoint currentPos = grid.getLocation(this);
		if ( currentPos == null ) return false;
		
		return ( goingPoint.getX() == currentPos.getX() && goingPoint.getY() == currentPos.getY() );
	}
	
	public void ActOnArrival()
	{
		switch ( this.goingWhere )
		{
			case Explore:
					// wat?
				break;
			case ForFood:
					AskForFood();
				break;
			case Home:
					// TODO
				break;
			case HomeWithFood:
					DropFood();
				break;
			case Wpierdol:
				
				break;
			case Uknown: break;
			default: break;
		}
		this.goingPoint = null;
		this.isGoingSomewhere = false;
		this.goingWhere = GoingWhere.Uknown;
	}
	
	private void AskForFood()
	{
		Maw m = MawFinder.Instance().GetMaw( this.playerID );
		AskForFoodMessage message = new AskForFoodMessage("can I haz food?");
		sendMessage( m, message );
	}

	public void ReceiveFood()
	{
		this.goingPoint = null;
		this.isGoingSomewhere = false;
		this.goingWhere = GoingWhere.Uknown;
		
		// max the food ^^
		this.food = Constants.MOBILE_STARTING_FOOD;
	}

	private void DropFood( )
	{
		if ( carriedStuff != null )
		{
			Maw m = MawFinder.Instance().GetMaw( this.playerID );
			m.ReceiveFood( carriedStuff );
			this.carriedStuff = null;}
	}
	
	public void Aggro()
	{
		if(!isStarving) {
			this.agression++;
			this.food = Constants.MOBILE_STARTING_FOOD;
			isStarving = true;
			
		}
		else {
			Die();
		}
	}
	
	private void GoHome()
	{
		this.setGoingSomewhere(true);
		goingPoint = MawFinder.Instance().GetMawPosition( this.playerID );
	}

	public void moveTowardsBro(GridPoint gp)
	{
		this.setGoingSomewhere(true);
		this.goingPoint = gp;
		if(grid.getLocation(this) == gp)
			this.setMove(false);
		
	}
	private int CallForBros(int neededBros)
	{
		//System.out.println("bros count: " + bros.size() + " needed bros: " + neededBros);
		GridPoint pt = grid.getLocation ( this );
		int x = 5, y = 5;
		
		GridCellNgh <Mobile> nghCreator = new GridCellNgh <Mobile>( grid , pt ,
		Mobile . class , x , y);
		List <GridCell<Mobile>> gridCells = nghCreator.getNeighborhood ( true );
		
			for ( GridCell <Mobile> cell : gridCells ) {
				for(Object obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY() )){
					if(obj instanceof Mobile && (Mobile)obj != this) {
					if (bros.size() < neededBros) {
						Mobile mobile = (Mobile)obj;
						if(mobile.playerID == this.playerID && !bros.contains(mobile)) {
							System.out.println("found bro!");
							AskForHelpMessage pls = new AskForHelpMessage("halp pls", pt);
							sendMessage(mobile, pls);
							bros.add(mobile);
						}
					}
					if(bros.size() == neededBros)
						return bros.size();
				}
			}
		}
		return bros.size();
	}
	
	@SuppressWarnings("unchecked")
	private void PickUpFood(List<Food> foodHere) {
		int found = 0;
		// food with highest power-weight ratio
		Collections.sort( foodHere );
		
		// iterate over foodHere
		for ( Food food : foodHere )
		{
			// check if food too heavy
			if ( carriedStuff == null &&  food.getWeight() <= this.carryCapacity && !food.isPicked() )
			{	// lift
				StartCarrying( food );
				this.goingWhere = GoingWhere.HomeWithFood;
				GoHome();
				break; 
			} else
			{
				//this.move = false;
				
				//int neededBros = (int) Math.ceil(food.getWeight()/(this.carryCapacity - this.carriedWeight));
				//if(bros.size() < neededBros) 
				//	CallForBros(neededBros);
				
				//System.out.println("Enough bros!");
				
			}
		}
	}

	public void Explore()
	{
		if(isFighting)
			return;
		// get current location in grid
		//food;
		GridPoint gp = grid.getLocation(this);
		
		// TODO: remember food in vicinity
	
		List<Food> foodHere = FoodAtPoint( gp );
		if ( foodHere.size() > 0 ) PickUpFood( foodHere );
		
		// calculate gohome desire
		if ( getGoHomeDesire( gp ) > Constants.MOBILE_GO_HOME_THRESHOLD )
		{
			this.goingWhere = GoingWhere.ForFood;
			GoHome();
		}
		
		// move randomly
		MoveRandomly( gp );
	}
	
	private double getGoHomeDesire( GridPoint gp ) {
		return 
				Math.abs(Constants.MOBILE_STARTING_FOOD - food ) + 
				MawFinder.Instance().GetDistanceToMaw(this.playerID, gp.getX(), gp.getY());
	}

	private void MoveRandomly( GridPoint gp )
	{
		// get random next position
		int randX = ( RandomHelper.nextIntFromTo(-1, 1) );
		int randY = ( RandomHelper.nextIntFromTo(-1, 1) );

		randX += gp.getX(); randY += gp.getY();
		
		// catch out of bounds
		GridDimensions spaceDim = grid.getDimensions();
		
		// X too big
		if ( randX >= spaceDim.getWidth() ) randX = spaceDim.getWidth() - 1;
		// Y too big
		if ( randY >= spaceDim.getHeight() ) randY = spaceDim.getHeight() - 1;
		
		// X too small
		if ( randX < 1 ) randX = 1;
		// Y too small
		if ( randY < 1 ) randY = 1;
		
		GridPoint randomGP = new GridPoint( randX, randY );
		this.moveTowards(randomGP);
	}

	public void moveTowards( GridPoint gp )
	{
		// only move if not already there
		if ( !gp.equals( grid.getLocation(this) ) )
		{
			NdPoint thisLocation = space.getLocation(this);
			NdPoint goalLocation = new NdPoint ( gp.getX (), gp.getY ());
			double angle = SpatialMath.calcAngleFor2DMovement( space, thisLocation, goalLocation );
			space.moveByVector(this, 1, angle, 0);
			thisLocation = space.getLocation(this);	
			// WARNING: without Math.round this gets cut and has a converging behavior when running randomly around
			grid.moveTo(this, (int)Math.round(thisLocation.getX()), (int)Math.round(thisLocation.getY()) );
			
			food--;
		}
	}
	
	/**
	 * @return the playerID
	 */
	@Parameter(displayName = "Player", usageName = "playerID")
	public int getPlayerID() {
		return playerID;
	}

	/**
	 * @param playerID the playerID to set
	 */
	public void setPlayerID(int playerID) {
		this.playerID = playerID;
	}
	
	/**
	 * @return the strength
	 */
	@Parameter(displayName = "strength", usageName = "strength")
	public float getStrength() {
		return this.strength;
	}
	/**
	 * @param strength the strength to set
	 */
	public void setStrength(float strength) {
		this.strength = strength;
		setDamage(Constants.MOBILE_ATTACK + strength * Constants.STRENGTH_MULTIPLY_FACTOR );
	}
	
	/**
	 * @return the experience
	 */
	@Parameter(displayName = "experience", usageName = "experience")
	public int getExperience() {
		return experience;
	}
	/**
	 * @param experience the experience to set
	 */
	public void setExperience(int experience) {
		this.experience = experience;
	}
	
	
	/**
	 * @return the intelligence
	 */
	@Parameter(displayName = "intelligence", usageName = "intelligence")
	public int getIntelligence() {
		return intelligence;
	}
	/**
	 * @param intelligence the intelligence to set
	 */
	public void setIntelligence(int intelligence) {
		this.intelligence = intelligence;
	}
	
	
	/**
	 * @return the carryCapacity
	 */
	@Parameter(displayName = "carry capacity", usageName = "carry capacity")
	public int getCarryCapacity() {
		return carryCapacity;
	}
	/**
	 * @param carryCapacity the carryCapacity to set
	 */
	public void setCarryCapacity(int carryCapacity) {
		this.carryCapacity = carryCapacity;
	}
	
	
	/**
	 * @return the diplomacySkill
	 */
	@Parameter(displayName = "diplomacy", usageName = "diplomacySkill")
	public int getDiplomacySkill() {
		return diplomacySkill;
	}
	/**
	 * @param diplomacySkill the diplomacySkill to set
	 */
	public void setDiplomacySkill(int diplomacySkill) {
		this.diplomacySkill = diplomacySkill;
	}

	/**
	 * @return the isGoingSomewhere
	 */
	public boolean isGoingSomewhere() {
		return isGoingSomewhere;
	}

	/**
	 * @param isGoingSomewhere the isGoingSomewhere to set
	 */
	public void setGoingSomewhere(boolean isGoingSomewhere) {
		this.isGoingSomewhere = isGoingSomewhere;
	}

	public float getSize() {
		return this.size;
	}

	public void setSize(float size) {
		this.size = size;
	}

	/**
	 * Return agents in given neighborhood
	 * @param extentX
	 * @param extentY
	 */
	public List<Agent> getAgentsInVicinity(int extentX, int extentY){
		List<Agent> vicinity = new ArrayList<Agent>();
		
		// get the grid location of this Human
		GridPoint pt = grid.getLocation(this);
		// use the GridCellNgh class to create GridCells for
		// the surrounding neighborhood .
		GridCellNgh <Agent> nghCreator = new GridCellNgh <Agent>(grid , pt,
		Agent.class , extentX , extentY);
		
		List <GridCell<Agent>> gridCells = nghCreator.getNeighborhood(true);
		
		for ( GridCell <Agent> cell : gridCells ) {
			for(Object obj : grid.getObjectsAt(cell.getPoint().getX(), cell.getPoint().getY() )){
				if(obj instanceof Agent || 
						(obj instanceof Mobile && (Mobile)obj != this)){
					Agent agent = (Agent)obj;
					vicinity.add(agent);
				}	
			}
		}
		return vicinity;
	}

	/**
	 * Seeks for knowledge, adds interesting points in the map
	 * and saves it in mobile's knowledge base
	 */
	public void seekForKnowledge(){
		List<Agent> vicinity = getAgentsInVicinity(Constants.MOBILE_VICINITY_X, Constants.MOBILE_VICINITY_Y);
		
		for(int i =0;i<vicinity.size();i++){
		
			Agent agent = vicinity.get(i);

			InformationType infoType = KnowledgeBase.GetInfoType(agent);
			
			// if info is interesting add it
			if(infoType != InformationType.GARBAGE){

				GridPoint pt = grid.getLocation(this);
				
				// time when it knowledge was added TODO
				
				double tickCount = RunEnvironment.getInstance().getCurrentSchedule().getTickCount();
				
				Information info = new Information(agent, infoType, tickCount, pt);
				
				this.knowledgeBase.addInformation(info);
				
				if(Constants.DEBUG_MODE){
					System.out.println("*********************************************************");
					System.out.println("Agent #" + this.id +" Gained knowledge");
					System.out.println("What: " + infoType.toString());
					System.out.println("Where: [" + pt.getX() + + pt.getY() +"] ");
					System.out.println("When: " + tickCount);
					System.out.println("********************************************************* \n\n");
				}
			}
		}
	
	}

	/**
	 * @return the agression
	 */
	public int getAgression() {
		return agression;
	}


	/**
	 * @param agression the agression to set
	 */
	public void setAgression(int agression) {
		this.agression = agression;
	}

	/**
	 * @return the isInFormation
	 */
	public boolean isInFormation() {
		return isInFormation;
	}


	/**
	 * @param isInFormation the isInFormation to set
	 */
	public void setInFormation(boolean isInFormation) {
		this.isInFormation = isInFormation;
	}
	public boolean getMove() {
		return move;
	}


	public void setMove(boolean move) {
		this.move = move;
	}
}

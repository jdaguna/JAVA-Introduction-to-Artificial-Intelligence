// J. Joshua Daguna, IV
//
// This class implements a layer of processing units.
//
// David Noelle -- Tue Apr 24 15:51:19 PDT 2007
// Assisted by Angelo Kyrilov 12.13.2013



import java.util.*;


public class Layer {

    public int n;                         // number of units in the layer
    public double min;                    // minimum activation level
    public double max;                    // maximum activation level
    public Vector net;                    // net input levels of units
    public Vector act;                    // activation levels of units
    public Vector targ;                   // current target for output layers
    public Vector delta;                  // unit delta values
    public Vector bias;                   // unit bias weights
    public Vector biasDelta;              // bias weight delta values
    public List<Projection> inputs;       // projections into this layer
    public List<Projection> outputs;      // projections out of this layer

    // Default constructor ...
    public Layer() {
	this.n = -1;
	this.min = 0.0;
	this.max = 1.0;
	this.net = null;
	this.act = null;
	this.targ = null;
	this.delta = null;
	this.bias = null;
	this.biasDelta = null;
	this.inputs = new LinkedList<Projection>();
	this.outputs = new LinkedList<Projection>();
    }

    // Constructor with size and activation range specified ...
    public Layer(int size, double min, double max) {
	if ((size > 0) && (min < max)) {
	    this.n = size;
	    this.min = min;
	    this.max = max;
	    this.net = new Vector(size);
	    this.act = new Vector(size);
	    this.targ = null;
	    this.delta = new Vector(size);
	    this.bias = new Vector(size);
	    this.biasDelta = new Vector(size);
	    this.inputs = new LinkedList<Projection>();
	    this.outputs = new LinkedList<Projection>();
	} else {
	    this.n = -1;
	    this.min = 0.0;
	    this.max = 1.0;
	    this.net = null;
	    this.act = null;
	    this.targ = null;
	    this.delta = null;
	    this.bias = null;
	    this.biasDelta = null;
	    this.inputs = new LinkedList<Projection>();
	    this.outputs = new LinkedList<Projection>();
	}
    }

    // Constructor with size specified ...
    public Layer(int size) {
	this(size, 0.0, 1.0);
    }

    // resize -- Change the size of this layer.  Activation and delta values
    // may be lost in this process.  All input and output projections are
    // discarded.
    public void resize(int size) {
	if (size > 0) {
	    n = size;
	    net = new Vector(size);
	    act = new Vector(size);
	    targ = null;
	    delta = new Vector(size);
	    bias = new Vector(size);
	    biasDelta = new Vector(size);
	    inputs = new LinkedList<Projection>();
	    outputs = new LinkedList<Projection>();
	} else {
	    n = -1;
	    net = null;
	    act = null;
	    targ = null;
	    delta = null;
	    bias = null;
	    biasDelta = null;
	    inputs = new LinkedList<Projection>();
	    outputs = new LinkedList<Projection>();
	}
    }

    // setActivationRange -- Change the minimum and maximum activation
    // values for the units in this layer.
    public void setActivationRange(double min, double max) {
	if (min < max) {
	    this.min = min;
	    this.max = max;
	}
    }

    // addInputProjection -- Add the given projection to the list of
    // projections coming into this layer.
    public void addInputProjection(Projection p) {
	if (p.outputN == n) {
	    inputs.add(p);
	    p.output = this;
	}
    }

    // addOutputProjection -- Add the given projection to the list of
    // projections coming out of this layer.
    public void addOutputProjection(Projection p) {
	if (p.inputN == n) {
	    outputs.add(p);
	    p.input = this;
	}
    }

    // clearNetInputs -- Set all net input values in the layer to zero.
    public void clearNetInputs() {
	for (int i = 0; i < net.dim(); i++)
	    net.set(i, 0.0);
    }

    // clearActivation -- Set all activation values in the layer to zero.
    public void clearActivation() {
	for (int i = 0; i < act.dim(); i++)
	    act.set(i, 0.0);
    }

    // clearUnitDeltas -- Set all unit delta values in the layer to zero.
    public void clearUnitDeltas() {
	for (int i = 0; i < delta.dim(); i++)
	    delta.set(i, 0.0);
    }

    // clearBiasDeltas -- Set all bias weight deltas in the layer to zero.
    public void clearBiasDeltas() {
	for (int i = 0; i < biasDelta.dim(); i++)
	    biasDelta.set(i, 0.0);
    }

    // randomizeBiases -- Set the bias weights to random values sampled
    // uniformly from the given range.
    public void randomizeBiases(double min, double max) {
	bias.randomize(min, max);
    }

    // loadInput -- Load the given vector of values into the activation 
    // vector for this layer.  This allows this layer to act as an input
    // layer.  Return false on error.
    public boolean loadInput(Vector v) {
	if (act.valid() && v.valid() && (act.dim() == v.dim())) {
	    for (int i = 0; i < act.dim(); i++)
		act.set(i, v.get(i));
	    return (true);
	} else {
	    // The vectors don't match ...
	    return (false);
	}
    }

    // loadTarget -- Load the given vector of values into the target vector
    // for this layer.  Specifically, just direct the target reference for
    // this layer to the given vector, allocating no new storage.  This
    // allows this layer to act as an output layer.  Return false on error.
    public boolean loadTarget(Vector v) {
	if (act.valid() && v.valid() && (act.dim() == v.dim())) {
	    targ = v;
	    return (true);
	} else {
	    // The vector doesn't match the layer ...
	    return (false);
	}
    }

    // computeActivation -- Calculate the activation values of the units in
    // this layer based on their inputs and bias weights.
    public void computeActivation() {
	if (!(inputs.isEmpty())) {
	    // This is not an input layer, so we can update it ...
	    // Add in the bias values to the net inputs ...
	    net = net.copy(bias);
	    // Sum up the contributions of each projection ...
	    for (Projection p : inputs)
		net = net.sum(p.W.product(p.input.act));
	    act = net.squash(min, max);
	}
    }

    // computeOutputDelta -- Calculate the unit delta values for this
    // output layer.
    public void computeOutputDelta() {
    /*
    	matlab code::
        In=Inputs(n,:);
        Targ=Targets(n,:);
           actOutput=1./(1+(exp(-(actHidden*Weight2+BiasO)))); %output layer activations
    
        Err= Targ-actOutput; %calculate error
         errs(n)=Err.^2;
        
        deltaOut=(Err).*(actOutput.*(1-actOutput)); %delta for output
        deltaHid=(actHidden.*(1-actHidden).*(Weight2'*deltaOut)); %delta for hidden
     */
    	Vector err = targ.difference(act);
    	Vector g = net.derivative(min, max);
    	
    	for(int i = 0; i<=n; i++){
    		delta.set(i,err.get(i) * g.get(i));
    	}
    	
    }

    // computeHiddenDelta -- Calculate the unit delta values for this hidden
    // layer.
    public void computeHiddenDelta() {
    	Vector err = new Vector(n, 0.0);

    	for(Projection p:outputs){
    		err = err.sum(p.W.transpose().product(p.output.delta));
    	}
    	
    	
    	Vector g = net.derivative(min, max);
    	  	
    	for(int i = 0; i<=n; i++){
    		delta.set(i,err.get(i) * g.get(i));
    	}
    	


    }

    // computeDelta -- Calculate the unit delta values for this layer.
    public void computeDelta() {
	if (outputs.isEmpty()) {
	    // This is an output layer ...
	    computeOutputDelta();
	} else {
	    // No point in computing unit delta values for input layers, 
	    // as those values are not used ...
	    if (!(inputs.isEmpty())) {
		// This is a hidden layer ...
		computeHiddenDelta();
	    }
	}
    }

    // incrementBiasDeltas -- Update how much the bias weights should
    // change, as a function of the current unit delta values.  Note that
    // this increments the current bias weight delta values, allowing
    // multiple weight changes to be "summed up".  Note also that these
    // weight delta values are prior to the application of the learning rate.
    public void incrementBiasDeltas() {
	biasDelta = biasDelta.sum(delta);
    }

    // updateBiases -- Update the bias weights.
    public void updateBiases(double learningRate) {
	bias = bias.sum(biasDelta.multiplyByScalar(learningRate));
    }


}


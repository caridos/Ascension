package net.thejadeproject.ascension.refactor_packages.paths.data.foundation.stability;

public class LnStabilityHandler extends GenericCalculatedStabilityHandler{

    private int maxProgress;

    public LnStabilityHandler(int maxProgress){
        super(input->Math.log(6*input+1),0,10,maxProgress);
        this.maxProgress = maxProgress;
    }
}
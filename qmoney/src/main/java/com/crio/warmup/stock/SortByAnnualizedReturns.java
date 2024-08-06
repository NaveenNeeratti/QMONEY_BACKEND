package com.crio.warmup.stock;

import java.util.Comparator;
import com.crio.warmup.stock.dto.AnnualizedReturn;

public class SortByAnnualizedReturns implements Comparator<AnnualizedReturn> {
    @Override
    public int compare(AnnualizedReturn arg0, AnnualizedReturn arg1) {
        if(arg1.getAnnualizedReturn() >arg0.getAnnualizedReturn()){
            return 1;
        }
        else if(arg1.getAnnualizedReturn() < arg0.getAnnualizedReturn()){
            return -1;
        }
        return 0;
    }
}

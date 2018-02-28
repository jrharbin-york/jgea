/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.jgea.core.listener.collector;

import it.units.malelab.jgea.core.listener.event.EvolutionEvent;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 * @author eric
 */
public class Static implements DataCollector {
  
  private final Map<String, Object> values;

  public Static(Map<String, Object> values) {
    this.values = values;
  }

  @Override
  public Map<String, String> getFormattedNames() {
    return values.entrySet().stream()
            .collect(Collectors.toMap(Map.Entry::getKey, e -> "%"+e.getValue().toString().length()+"."+e.getValue().toString().length()+"s"));
  }

  @Override
  public Map<String, Object> collect(EvolutionEvent evolutionEvent) {
    return values;
  }    
  
}
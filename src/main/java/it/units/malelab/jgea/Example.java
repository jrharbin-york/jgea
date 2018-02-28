/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package it.units.malelab.jgea;

import com.google.common.collect.Lists;
import com.sun.xml.internal.ws.policy.sourcemodel.wspolicy.XmlToken;
import it.units.malelab.jgea.core.Individual;
import it.units.malelab.jgea.core.Node;
import it.units.malelab.jgea.core.ProblemWithValidation;
import it.units.malelab.jgea.core.Sequence;
import it.units.malelab.jgea.core.evolver.DeterministicCrowdingEvolver;
import it.units.malelab.jgea.core.evolver.FitnessSharingDivideAndConquerEvolver;
import it.units.malelab.jgea.core.evolver.StandardEvolver;
import it.units.malelab.jgea.core.evolver.stopcondition.ElapsedTime;
import it.units.malelab.jgea.core.evolver.stopcondition.FitnessEvaluations;
import it.units.malelab.jgea.core.evolver.stopcondition.PerfectFitness;
import it.units.malelab.jgea.core.fitness.ClassificationFitness;
import it.units.malelab.jgea.core.function.Function;
import it.units.malelab.jgea.core.function.Reducer;
import it.units.malelab.jgea.core.listener.Listener;
import it.units.malelab.jgea.core.listener.collector.Basic;
import it.units.malelab.jgea.core.listener.collector.BestInfo;
import it.units.malelab.jgea.core.listener.collector.BestPrinter;
import it.units.malelab.jgea.core.listener.collector.FunctionOfBest;
import it.units.malelab.jgea.core.listener.collector.Diversity;
import it.units.malelab.jgea.core.listener.collector.Population;
import it.units.malelab.jgea.core.operator.GeneticOperator;
import it.units.malelab.jgea.core.ranker.ComparableRanker;
import it.units.malelab.jgea.core.ranker.FitnessComparator;
import it.units.malelab.jgea.core.ranker.ParetoRanker;
import it.units.malelab.jgea.core.ranker.selector.Tournament;
import it.units.malelab.jgea.core.ranker.selector.Worst;
import it.units.malelab.jgea.core.util.Pair;
import it.units.malelab.jgea.core.util.WithNames;
import it.units.malelab.jgea.distance.Distance;
import it.units.malelab.jgea.distance.Edit;
import it.units.malelab.jgea.grammarbased.GrammarBasedProblem;
import it.units.malelab.jgea.grammarbased.cfggp.RampedHalfAndHalf;
import it.units.malelab.jgea.grammarbased.cfggp.StandardTreeCrossover;
import it.units.malelab.jgea.grammarbased.cfggp.StandardTreeMutation;
import it.units.malelab.jgea.problem.booleanfunction.EvenParity;
import it.units.malelab.jgea.problem.booleanfunction.element.Element;
import it.units.malelab.jgea.problem.classification.AbstractClassificationProblem;
import it.units.malelab.jgea.problem.classification.BinaryRegexClassification;
import it.units.malelab.jgea.problem.classification.GrammarBasedRegexClassification;
import it.units.malelab.jgea.problem.classification.RegexClassification;
import it.units.malelab.jgea.problem.extraction.AbstractExtractionProblem;
import it.units.malelab.jgea.problem.extraction.BinaryRegexExtraction;
import it.units.malelab.jgea.problem.extraction.ExtractionFitness;
import it.units.malelab.jgea.problem.extraction.RegexGrammar;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author eric
 */
public class Example extends Worker {

  public Example(String[] args) throws FileNotFoundException {
    super(args);
  }

  public final static void main(String[] args) throws FileNotFoundException {
    new Example(args);
  }

  public void run() {
    try {
      //binaryRegexStandard(executorService);
      //binaryRegexDC(executorService);
      //binaryRegexFSDC(executorService);
      binaryRegexExtractionStandard(executorService);
    } catch (IOException | InterruptedException | ExecutionException ex) {
      Logger.getLogger(Example.class.getName()).log(Level.SEVERE, null, ex);
    }
  }

  private void parity(ExecutorService executor) throws IOException, InterruptedException, ExecutionException {
    final GrammarBasedProblem<String, List<Node<Element>>, Double> p = new EvenParity(5);
    Map<GeneticOperator<Node<String>>, Double> operators = new LinkedHashMap<>();
    operators.put(new StandardTreeMutation<>(12, p.getGrammar()), 0.2d);
    operators.put(new StandardTreeCrossover<>(12), 0.8d);
    StandardEvolver<Node<String>, List<Node<Element>>, Double> evolver = new StandardEvolver<>(
            500,
            new RampedHalfAndHalf<>(3, 12, p.getGrammar()),
            new ComparableRanker(new FitnessComparator<>()),
            p.getSolutionMapper(),
            operators,
            new Tournament<>(3),
            new Worst<>(),
            500,
            true,
            Lists.newArrayList(new FitnessEvaluations(100000), new PerfectFitness<>(p.getFitnessFunction())),
            10000,
            false
    );
    Random r = new Random(1);
    evolver.solve(p, r, executor,
            Listener.onExecutor(listener(
                            new Basic(),
                            new Population(),
                            new BestInfo<>(
                                    (f, l) -> Collections.singletonMap("", f),
                                    (n, l) -> "%6.4f"),
                            new Diversity(),
                            new BestPrinter(null, "%s")
                    ), executor)
    );
  }

  private void binaryRegexStandard(ExecutorService executor) throws IOException, InterruptedException, ExecutionException {
    GrammarBasedProblem<String, String, List<Double>> p = new BinaryRegexClassification(
            50, 100, 1,
            5, 0,
            ClassificationFitness.Metric.BALANCED_ERROR_RATE, ClassificationFitness.Metric.CLASS_ERROR_RATE,
            RegexGrammar.Option.ANY, RegexGrammar.Option.ENHANCED_CONCATENATION, RegexGrammar.Option.OR
    );
    Map<GeneticOperator<Node<String>>, Double> operators = new LinkedHashMap<>();
    operators.put(new StandardTreeMutation<>(15, p.getGrammar()), 0.2d);
    operators.put(new StandardTreeCrossover<>(15), 0.8d);
    StandardEvolver<Node<String>, String, List<Double>> evolver = new StandardEvolver<>(
            100,
            new RampedHalfAndHalf<>(3, 15, p.getGrammar()),
            new ParetoRanker<>(),
            p.getSolutionMapper(),
            operators,
            new Tournament<>(3),
            new Worst(),
            100,
            true,
            Lists.newArrayList(new ElapsedTime(90, TimeUnit.SECONDS), new PerfectFitness<>(p.getFitnessFunction())),
            10000,
            false
    );
    Random r = new Random(1);
    evolver.solve(p, r, executor,
            Listener.onExecutor(listener(new Basic(),
                            new Population(),
                            new BestInfo<>(
                                    BestInfo.fromNames((WithNames) p.getFitnessFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "learning",
                                    ((ClassificationFitness) ((AbstractClassificationProblem) p).getFitnessFunction()).changeMetric(ClassificationFitness.Metric.ERROR_RATE).cached(10000),
                                    BestInfo.fromNames((WithNames) ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "validation",
                                    ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction().cached(10000),
                                    BestInfo.fromNames((WithNames) ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction()),
                                    (n, l) -> "%5.3f"),
                            new Diversity(),
                            new BestPrinter(null, "%s")
                    ), executor
            )
    );
  }

  private void binaryRegexDC(ExecutorService executor) throws IOException, InterruptedException, ExecutionException {
    GrammarBasedProblem<String, String, List<Double>> p = new BinaryRegexClassification(
            50, 100, 1,
            5, 0,
            ClassificationFitness.Metric.BALANCED_ERROR_RATE, ClassificationFitness.Metric.CLASS_ERROR_RATE,
            RegexGrammar.Option.ANY, RegexGrammar.Option.ENHANCED_CONCATENATION, RegexGrammar.Option.OR
    );
    Map<GeneticOperator<Node<String>>, Double> operators = new LinkedHashMap<>();
    operators.put(new StandardTreeMutation<>(15, p.getGrammar()), 0.2d);
    operators.put(new StandardTreeCrossover<>(15), 0.8d);
    Distance<Sequence<Character>> edit = new Edit<>();
    Distance<String> editString = (s1, s2, l) -> (edit.apply(
            Sequence.from(s1.chars().mapToObj(c -> (char) c).toArray(Character[]::new)),
            Sequence.from(s2.chars().mapToObj(c -> (char) c).toArray(Character[]::new))
    ));
    Distance<Individual<Node<String>, String, List<Double>>> distance = (Individual<Node<String>, String, List<Double>> i1, Individual<Node<String>, String, List<Double>> i2, Listener l) -> (editString.apply(
            i1.getSolution(),
            i2.getSolution()
    ));
    StandardEvolver<Node<String>, String, List<Double>> evolver = new DeterministicCrowdingEvolver<>(
            distance.cached(10000),
            500,
            new RampedHalfAndHalf<>(3, 15, p.getGrammar()),
            new ParetoRanker<>(),
            p.getSolutionMapper(),
            operators,
            new Tournament<>(3),
            new Worst(),
            Lists.newArrayList(new ElapsedTime(90, TimeUnit.SECONDS), new PerfectFitness<>(p.getFitnessFunction())),
            10000,
            false
    );
    Random r = new Random(1);
    evolver.solve(p, r, executor,
            Listener.onExecutor(listener(new Basic(),
                            new Population(),
                            new BestInfo<>(
                                    BestInfo.fromNames((WithNames) p.getFitnessFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "learning",
                                    ((ClassificationFitness) ((AbstractClassificationProblem) p).getFitnessFunction()).changeMetric(ClassificationFitness.Metric.ERROR_RATE).cached(10000),
                                    BestInfo.fromNames((WithNames) ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "validation",
                                    ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction().cached(10000),
                                    BestInfo.fromNames((WithNames) ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction()),
                                    (n, l) -> "%5.3f"),
                            new Diversity(),
                            new BestPrinter(null, "%s")
                    ), executor
            )
    );
  }

  private void binaryRegexFSDC(ExecutorService executor) throws IOException, InterruptedException, ExecutionException {
    GrammarBasedProblem<String, String, List<Double>> p = new BinaryRegexClassification(
            100, 200, 1,
            5, 0,
            ClassificationFitness.Metric.BALANCED_ERROR_RATE, ClassificationFitness.Metric.CLASS_ERROR_RATE,
            RegexGrammar.Option.ANY, RegexGrammar.Option.ENHANCED_CONCATENATION
    );
    Map<GeneticOperator<Node<String>>, Double> operators = new LinkedHashMap<>();
    operators.put(new StandardTreeMutation<>(15, p.getGrammar()), 0.2d);
    operators.put(new StandardTreeCrossover<>(15), 0.8d);
    Distance<List<RegexClassification.Label>> semanticsDistance = (l1, l2, listener) -> {
      double count = 0;
      for (int i = 0; i < Math.min(l1.size(), l2.size()); i++) {
        if (!l1.get(i).equals(l2.get(i))) {
          count = count + 1d;
        }
      }
      return count / (double) Math.min(l1.size(), l2.size());
    };
    Reducer<Pair<String, List<RegexClassification.Label>>> reducer = (p0, p1, listener) -> {
      String s = p0.first() + "|" + p1.first();
      List<RegexClassification.Label> ored = new ArrayList<>(Math.min(p0.second().size(), p1.second().size()));
      for (int i = 0; i < Math.min(p0.second().size(), p1.second().size()); i++) {
        if (p0.second().get(i).equals(RegexClassification.Label.FOUND) || p1.second().get(i).equals(RegexClassification.Label.FOUND)) {
          ored.add(RegexClassification.Label.FOUND);
        } else {
          ored.add(RegexClassification.Label.NOT_FOUND);
        }
      }
      return Pair.build(s, ored);
    };
    StandardEvolver<Node<String>, String, List<Double>> evolver = new FitnessSharingDivideAndConquerEvolver<>(
            reducer,
            semanticsDistance,
            500,
            new RampedHalfAndHalf<>(3, 15, p.getGrammar()),
            new ParetoRanker<>(),
            p.getSolutionMapper(),
            operators,
            new Tournament<>(3),
            new Worst(),
            500,
            true,
            Lists.newArrayList(new ElapsedTime(90, TimeUnit.SECONDS), new PerfectFitness<>(p.getFitnessFunction())),
            10000
    );
    Random r = new Random(1);
    evolver.solve(p, r, executor,
            Listener.onExecutor(listener(new Basic(),
                            new Population(),
                            new BestInfo<>(
                                    BestInfo.fromNames((WithNames) p.getFitnessFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "learning",
                                    ((ClassificationFitness) ((AbstractClassificationProblem) p).getFitnessFunction()).changeMetric(ClassificationFitness.Metric.ERROR_RATE).cached(10000),
                                    BestInfo.fromNames((WithNames) ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "validation",
                                    ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction().cached(10000),
                                    BestInfo.fromNames((WithNames) ((ProblemWithValidation<String, List<Double>>) p).getValidationFunction()),
                                    (n, l) -> "%5.3f"),
                            new Diversity(),
                            new BestPrinter(null, "%s")
                    ), executor
            )
    );
  }

  private void binaryRegexExtractionStandard(ExecutorService executor) throws IOException, InterruptedException, ExecutionException {
    GrammarBasedProblem<String, String, List<Double>> p = new BinaryRegexExtraction(
            10, 1,
            false, new HashSet<>(Arrays.asList(RegexGrammar.Option.ANY, RegexGrammar.Option.ENHANCED_CONCATENATION, RegexGrammar.Option.OR)),
            5, 0,
            ExtractionFitness.Metric.ONE_MINUS_FM);
    Map<GeneticOperator<Node<String>>, Double> operators = new LinkedHashMap<>();
    operators.put(new StandardTreeMutation<>(15, p.getGrammar()), 0.2d);
    operators.put(new StandardTreeCrossover<>(15), 0.8d);
    StandardEvolver<Node<String>, String, List<Double>> evolver = new StandardEvolver<>(
            100,
            new RampedHalfAndHalf<>(3, 15, p.getGrammar()),
            new ParetoRanker<>(),
            p.getSolutionMapper(),
            operators,
            new Tournament<>(3),
            new Worst(),
            100,
            true,
            Lists.newArrayList(new ElapsedTime(90, TimeUnit.SECONDS), new PerfectFitness<>(p.getFitnessFunction())),
            10000,
            false
    );
    Random r = new Random(1);
    Function learningAssessmentFunction = ((ExtractionFitness) ((AbstractExtractionProblem) p).getFitnessFunction()).changeMetrics(ExtractionFitness.Metric.ONE_MINUS_PREC, ExtractionFitness.Metric.ONE_MINUS_REC, ExtractionFitness.Metric.CHAR_ERROR);
    Function validationAssessmentFunction = ((ExtractionFitness) ((AbstractExtractionProblem) p).getValidationFunction()).changeMetrics(ExtractionFitness.Metric.ONE_MINUS_FM, ExtractionFitness.Metric.ONE_MINUS_PREC, ExtractionFitness.Metric.ONE_MINUS_REC, ExtractionFitness.Metric.CHAR_ERROR);    
    evolver.solve(p, r, executor,
            Listener.onExecutor(listener(new Basic(),
                            new Population(),
                            new BestInfo<>(
                                    BestInfo.fromNames((WithNames) p.getFitnessFunction()),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "learning",
                                    learningAssessmentFunction.cached(10000),
                                    BestInfo.fromNames((WithNames)learningAssessmentFunction),
                                    (n, l) -> "%5.3f"),
                            new FunctionOfBest<>(
                                    "validation",
                                    validationAssessmentFunction.cached(10000),
                                    BestInfo.fromNames((WithNames)validationAssessmentFunction),
                                    (n, l) -> "%5.3f"),
                            new Diversity(),
                            new BestPrinter(null, "%s")
                    ), executor
            )
    );
  }

}
package com.tmjee.evo.workflow;

import java.util.*;

import static java.lang.String.format;

/**
 * @author tmjee
 */
public class FlowDiagramPrettyPrinter {

    private final Map<String, WorkflowStep> m;

    public FlowDiagramPrettyPrinter(Map<String, WorkflowStep> m) {
        this.m = m;
    }

    int lane =0;
    int i=0;
    Map<String, Node> nodesMap;
    Set<Integer> s = new TreeSet<>(); // all of the current lanes

    public void print() {
        lane = 0;
        i = 0;
        List<WorkflowStep> workflowSteps = new ArrayList<>(m.values());

        nodesMap = new LinkedHashMap<>();

        for (WorkflowStep step : workflowSteps) {
            String name = step.getName();
            WorkflowStep.Type type = step.getType();

            step.accept(new WorkflowStep.Visitor() {
                @Override
                public void setNextStep(String workflowStepName) {
                    Path p = new Path(lane++, null);
                    addOutgoingPath(p, name);
                    addIncommingPath(p, workflowStepName);
                }

                @Override
                public void setNextStep(String cond, String workflowStepName) {
                    Path p = new Path(lane++, cond);
                    addOutgoingPath(p, name);
                    addIncommingPath(p, workflowStepName);
                }
            });
        }

        for (Node n : nodesMap.values()) {
            //addOutgoingPaths(n);
            n.prettyPrint();
        }

        System.out.println("Legend:");
        for (Node n : nodesMap.values()) {
           System.out.println(format("\t%s - %s", n.i, n.name));
        }
    }

    private void addOutgoingPath(Path p, String workflowStepName) {
        findNode(workflowStepName, m.get(workflowStepName).getType()).addOutgoing(p);
    }

    private void addIncommingPath(Path p, String workflowStepName) {
        findNode(workflowStepName, m.get(workflowStepName).getType()).addIncomming(p);
    }

    private Node findNode(String workflowStepName, WorkflowStep.Type type) {
        if (!nodesMap.containsKey(workflowStepName)) {
            Node n = new Node(i++, workflowStepName, type);
            nodesMap.put(workflowStepName, n);
        }
        return nodesMap.get(workflowStepName);
    }
    private void addOutgoingPaths(Node n) {
        Set<Integer> o = n.outgoings.stream().mapToInt((p) -> p.lane).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        s.addAll(o);
    }
    private void removeIncommingPaths(Node n) {
        Set<Integer> o = n.incommings.stream().mapToInt((p) -> p.lane).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
        s.removeAll(o);
    }


    class Node {
        int i;
        String name;
        WorkflowStep.Type type;
        List<Path> incommings;
        List<Path> outgoings;

        Node(int i, String name, WorkflowStep.Type type) {
            this.i = i;
            this.name = name;
            this.type = type;
            incommings = new ArrayList<>();
            outgoings = new ArrayList<>();
        }
        void addIncomming(Path p){
            incommings.add(p);
        }
        void addOutgoing(Path p) {
            outgoings.add(p);
        }
        void prettyPrint() {
            switch(type) {
                case TASK:
                    //addOutgoingPaths(this);
                    System.out.print(format("  +-------+ "   ));prettyPrint_Path();printLine();
                    System.out.print(format("  |       | "   )); prettyPrint_OutgoingPath();printLine();
                    System.out.print(format("  |  %s    | ", i));prettyPrint_Path();printLine();
                    System.out.print(format("  |       | "   ));prettyPrint_IncommingPath();printLine();
                    System.out.print(format("  +-------+ "   ));prettyPrint_Path();printLine();
                    System.out.print(format("            "   ));prettyPrint_Path();printLine();
                    System.out.print(format("            "   ));prettyPrint_Path();printLine();
                    break;
                case DECISION:
                    System.out.print(format("   -----    "   ));prettyPrint_Path();printLine();
                    System.out.print(format("  /     \\   "   )); prettyPrint_OutgoingPath();printLine();
                    System.out.print(format(" /   %s   \\  ", i));prettyPrint_Path();printLine();
                    System.out.print(format(" \\       /  "   ));prettyPrint_IncommingPath();printLine();
                    System.out.print(format("  \\     /   "   ));prettyPrint_Path();printLine();
                    System.out.print(format("   -----    "   ));prettyPrint_Path();printLine();
                    System.out.print(format("            "   ));prettyPrint_Path();printLine();
                    break;
            }
        }
        void prettyPrint_IncommingPath() {
            Set<Integer> o = incommings.stream().mapToInt((p) -> p.lane).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
            if (o.isEmpty()) {
                prettyPrint_Path();
                return;
            }

            int max = Math.max(
                s.stream().max(Comparator.naturalOrder()).orElse(-1),
                o.stream().max(Comparator.naturalOrder()).orElse(-1));

            System.out.print("-<-");
            for (int a=0; a<=max;a++) {
                if (o.contains(a)) {
                    System.out.print("--+");
                    o.remove(a);
                    s.remove(a);
                } else if (!o.isEmpty() && s.contains(a)) {
                    System.out.print("-\\/");
                } else if (o.isEmpty() && s.contains(a)) {
                    System.out.print("  |");
                } else {
                    System.out.print("---");
                }
            }
        }

        void prettyPrint_OutgoingPath() {
            Set<Integer> o = outgoings.stream().mapToInt((p) -> p.lane).collect(TreeSet::new, TreeSet::add, TreeSet::addAll);
            if (o.isEmpty()) {
                prettyPrint_Path();
                return;
            }

            int max = Math.max(
                s.stream().max(Comparator.naturalOrder()).orElse(-1),
                o.stream().max(Comparator.naturalOrder()).orElse(-1));

            System.out.print("->-");

            for (int a=0; a<=max;a++) {
                if (o.contains(a)) {
                    System.out.print("--+");
                    s.add(a);
                } else if (!o.isEmpty() && s.contains(a)) {
                    System.out.print("-/\\");
                } else if (o.isEmpty() && s.contains(a)) {
                    System.out.print("  |");
                } else {
                    System.out.print("---");
                }
            }
        }

        void prettyPrint_Path() {
            System.out.print("   ");
            int max = s.stream().max(Comparator.naturalOrder()).orElse(-1);
            for (int a=0; a<=max; a++) {
                if (s.contains(a)) {
                    System.out.print(format("  |"));
                } else {
                    System.out.print(format("   "));
                }
            }
        }

        void printLine() {
            System.out.println();
        }
    }

    static class Path {
        int lane;
        String description;

        Path(int lane, String description) {
            this.lane = lane;
            this.description =description;
        }
    }
}

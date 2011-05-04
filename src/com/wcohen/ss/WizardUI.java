package com.wcohen.ss;

import java.util.*;
import java.io.*;
import java.awt.event.*;
import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;

import com.wcohen.util.gui.*;
import com.wcohen.util.*;
import com.wcohen.cls.expt.*;

import com.wcohen.ss.expt.*;
import com.wcohen.ss.api.*;

import jwf.*;


/**
 * Top-level GUI interface.
 */

public class WizardUI
{
	private static final Blocker DEFAULT_BLOCKER = new NullBlocker();
	private static final StringDistanceLearner DEFAULT_DISTANCE = new JaroWinklerTFIDF();

	//
	// main program to kick off the wizard
	//

	/** The entry point to the wizard. */
	public static void main(String args[]) 
	{
		JFrame frame = new JFrame("SecondString");
		frame.addWindowListener(new WindowAdapter() {
				public void windowClosing(WindowEvent we) {
					System.exit(0);
				}
			});
		Wizard wizard = new Wizard();
		wizard.addWizardListener(new WizardAdapter() {
				public void wizardFinished(Wizard wizard) {	System.exit(0);	}
				public void wizardCancelled(Wizard wizard) {  System.exit(0); }
			});
		frame.setContentPane(wizard);
		frame.pack();
		frame.setVisible(true);
		wizard.start( new PickTask() );
	}
	
	//
	// link up jwf.Wizard constructs to the wcohen.util.gui.Viewer mechanism
	//

	/** Wraps a WizardPanel in a viewer construct.
	 * Whatever is selected by the viewer is stored in the
	 * viewerContext map, under the given key.
	 */
	abstract private static class WizardViewer extends ComponentViewer 
	{
		private String myKey;
		final protected Map viewerContext;
		final private WizardPanel wizardPanel;
		public WizardViewer(String key,Map viewerContext)	
		{	
			this.myKey = key; 
			this.viewerContext = viewerContext;	
			this.wizardPanel = buildWizardPanel();
			//System.out.println("WizardViewer viewerContext: "+viewerContext);
		}
		public WizardViewer(String key)	
		{	
			this(key,new HashMap()); 
		}
		public WizardPanel getWizardPanel()	{	return wizardPanel;	}
		public JComponent componentFor(Object o) 	{ return wizardPanel;	}
		public boolean canHandle(int signal,Object argument,ArrayList senders) { return (signal==OBJECT_SELECTED);}
		public void handle(int signal,Object argument,ArrayList senders) 
		{
			if (signal==OBJECT_SELECTED) {
				System.out.println("selected "+argument);
				viewerContext.put(myKey,argument);
			}
		}
		/** Construct a WizardPanel that contains the viewer. */
		abstract public WizardPanel buildWizardPanel();
	}

	/** A special secondstring-specific type selector.
	 */
	private static class SSTypeSelector extends TypeSelector
	{
		private static Class[] ssClasses = {
			com.wcohen.ss.expt.NullBlocker.class
			,com.wcohen.ss.expt.TokenBlocker.class
			,com.wcohen.ss.expt.NGramBlocker.class
			,com.wcohen.ss.DirichletJS.class
			,com.wcohen.ss.JelinekMercerJS.class
			,com.wcohen.ss.Jaccard.class
			,com.wcohen.ss.Jaro.class
			,com.wcohen.ss.JaroWinkler.class
			,com.wcohen.ss.JaroWinklerTFIDF.class
			,com.wcohen.ss.Levenstein.class
			,com.wcohen.ss.MongeElkan.class
			,com.wcohen.ss.TokenFelligiSunter.class
			,com.wcohen.ss.TagLink.class
		};
		public SSTypeSelector(Class rootClass) { super(ssClasses,rootClass); }
	}

	//
	// define the steps of the wizard
	//

	/** pick the task
	 */
	private static class PickTask extends NullWizardPanel
	{
		private JRadioButton doExpt,reviewResult;
		public PickTask()
		{
			setBorder(new TitledBorder("SecondString: task selection"));
			add(new JLabel("Welcome to SecondString!"));
			add(new JLabel("Please select a task:"));
			doExpt = new JRadioButton("Perform an experiment", true);
			reviewResult = new JRadioButton("Review saved experimental results", false);
			ButtonGroup group = new ButtonGroup();
			add(doExpt);
			add(reviewResult);
			group.add(doExpt);
			group.add(reviewResult);
		}
		public boolean hasNext() { return true; }
		public boolean validateNext(java.util.List list) { return true; }
		public WizardPanel next() { 
			if (doExpt.isSelected()) return new PickBlocker().getWizardPanel();
			else return new PickExptFile().getWizardPanel();
		}
	}


	/** pick a saved match experiment file
	 */
	private static class PickExptFile extends WizardViewer
	{
		private MatchExpt matchExpt = null;
		private EvaluationGroup evalGroup = new EvaluationGroup();	
		private int groupSize = 0;

		public PickExptFile() { super("ExptFile"); }
		public WizardPanel buildWizardPanel() {	return new ExptFileWizardPanel(this);}
		private class ExptFileWizardPanel extends NullWizardPanel
		{
			public ExptFileWizardPanel(Viewer enclosingViewer)
			{
				setBorder(new TitledBorder("SecondString: saved experiment selection"));
				add(new JLabel("Please select one or more previously-saved experiments:"));
				final JTextField filePane = new JTextField(20);
				add(filePane);
				final ArrayList exptListData = new ArrayList();
				//final ListModel exptListModel = new AbstractListModel() {
				//public int getSize() { return exptListData.size(); }
				//public Object getElementAt(int i) { return exptListData.get(i); }
				//};
				final JList exptList = new JList();
				Dimension wide = new Dimension(600,100);
				exptList.setPreferredSize(wide);
				final JFileChooser chooser = new JFileChooser();
				add(new JButton(new AbstractAction("Browse") {
						public void actionPerformed(ActionEvent ev) {
							int returnVal = chooser.showOpenDialog(null);
							if (returnVal==JFileChooser.APPROVE_OPTION) {
								File exptFile = chooser.getSelectedFile();
								filePane.setText( exptFile.getName() );
								try {
									matchExpt = (MatchExpt)IOUtil.loadSerialized(exptFile);
									exptListData.add( matchExpt );
									evalGroup.add( exptFile.getName(), matchExpt.toEvaluation() );
									groupSize++;
									exptList.setListData( exptListData.toArray() );
								} catch (IOException ex) {
									System.err.println("can't load "+exptFile+": "+ex);
								}
							}
						}
					}));
				JPanel listPanel = new JPanel();
				listPanel.setPreferredSize(wide);
				listPanel.setBorder(new TitledBorder("Selected experiments"));
				JScrollPane scroller = new JScrollPane(exptList);
				scroller.setPreferredSize(wide);
				listPanel.add(scroller);
				add(listPanel);
			}
			public boolean hasNext() { return true; }
			public boolean validateNext(java.util.List list) 
			{ 
				list.add("You need to pick a file!"); 
				return groupSize>=1;
			}
			public WizardPanel next() { 
				//System.out.println("exptFile = "+exptFile);
				//System.out.println("matchexpt = "+matchExpt);
				if (groupSize==1) return new ResultWizardPanel(matchExpt.toGUI(), null); 
				else return new ResultWizardPanel(evalGroup.toGUI(), null);
			}
		}
	}

	/** pick the blocker
	 */
	private static class PickBlocker extends WizardViewer
	{
		public PickBlocker() { super("Blocker"); }
		public WizardPanel buildWizardPanel() {	return new BlockerWizardPanel(this);}
		private class BlockerWizardPanel extends NullWizardPanel
		{
			public BlockerWizardPanel(Viewer enclosingViewer)
			{
				setBorder(new TitledBorder("SecondString: blocker selection"));
				add(new JLabel("Please select a blocking strategy:"));
				SSTypeSelector selector = new SSTypeSelector(Blocker.class);
				selector.setSuperView(enclosingViewer);
				selector.receiveContent(DEFAULT_BLOCKER);
				viewerContext.put("Blocker",DEFAULT_BLOCKER);
				add(selector);
			}
			public boolean hasNext() { return true; }
			public boolean validateNext(java.util.List list) { return true; }
			public WizardPanel next() { return new PickDistance(viewerContext).getWizardPanel(); }
			public boolean hasHelp() { return true; }
			public void help() {
				String helpMsg = 
					"A \"blocker\" is used to make an initial guess about which pairs to match.\n"+
					"The NullBlocker picks all pairs; the TokenBlocker picks all pairs that share\n"+
					"a common token; and the NGramBlocker picks all pairs that share a common NGram.";
				JOptionPane.showMessageDialog(this,helpMsg,"Blocker Help",JOptionPane.INFORMATION_MESSAGE);
			}
		}
	}

	/** pick the blocker
	 */
	private static class PickDistance extends WizardViewer
	{
		public PickDistance(Map viewerContext) { super("Distance",viewerContext); }
		public WizardPanel buildWizardPanel() {	return new DistanceWizardPanel(this);}
		private class DistanceWizardPanel extends NullWizardPanel
		{
			public DistanceWizardPanel(Viewer enclosingViewer)
			{
				setBorder(new TitledBorder("SecondString: distance selection"));
				add(new JLabel("Please select a string distance:"));
				SSTypeSelector selector = new SSTypeSelector(StringDistanceLearner.class);
				selector.setSuperView(enclosingViewer);
				selector.receiveContent(DEFAULT_DISTANCE);
				viewerContext.put("Distance",DEFAULT_DISTANCE);
				add(selector);
				//System.out.println("PickDistance context: "+viewerContext);
			}
			public boolean hasNext() { return true; }
			public boolean validateNext(java.util.List list) { return true; }
			public WizardPanel next() { return new PickDatafile(viewerContext).getWizardPanel(); }
		}
	}
	
	/** pick the datafile
	 */
	private static class PickDatafile extends WizardViewer
	{
		public PickDatafile(Map viewerContext) { super("File",viewerContext); }
		public WizardPanel buildWizardPanel() {	return new DatafileWizardPanel(this);}
		private class DatafileWizardPanel extends NullWizardPanel
		{
			public DatafileWizardPanel(Viewer enclosingViewer)
			{
				setBorder(new TitledBorder("SecondString: datafile selection"));
				add(new JLabel("Please select a datafile:"));
				final JTextField filePane = new JTextField(20);
				add(filePane);
				final JFileChooser chooser = new JFileChooser();
				add(new JButton(new AbstractAction("Browse") {
						public void actionPerformed(ActionEvent ev) {
							int returnVal = chooser.showOpenDialog(null);
							if (returnVal==JFileChooser.APPROVE_OPTION) {
								viewerContext.put( "File", chooser.getSelectedFile() );
								filePane.setText( ((File)viewerContext.get("File")).getName() );
							}
						}
					}));
				//viewerContext.put("File",new File("data/birdScott2.txt"));
				//System.out.println("PickDatafile context: "+viewerContext);
			}
			public boolean hasNext() { return true; }
			public boolean validateNext(java.util.List list) { 
				list.add("You need to pick a file!");
				return viewerContext.get("File")!=null;
			}
			public WizardPanel next() { return new RunExperiment(viewerContext).getWizardPanel(); }
		}
	}

	/** Confirm selections & run experiment
	 */
	private static class RunExperiment extends WizardViewer
	{
		private MatchExpt matchExpt;
		private Viewer matchExptViewer;
		private boolean matchExptComplete = false;

		public RunExperiment(Map viewerContext) { super("none",viewerContext); }
		public WizardPanel buildWizardPanel() {	return new RunExperimentWizard(this);}
		private class RunExperimentWizard extends NullWizardPanel
		{
			public RunExperimentWizard(Viewer enclosingViewer)
			{
				//System.out.println("RunExperimentWizard context: "+viewerContext);
				setBorder(new TitledBorder("SecondString: confirm and run experiment"));
				setLayout(new GridBagLayout());
				GridBagConstraints gbc;
				String[] keys = new String[]{"Blocker","Distance","File"};
				Object[][] tableData = new Object[keys.length][2];
				for (int i=0; i<keys.length; i++) {
					tableData[i][0] = keys[i];
					tableData[i][1] = viewerContext.get(keys[i]);
				}
				JTable table = new JTable(tableData,new String[]{"Parameter","Value"});
				JScrollPane tableScroller = new JScrollPane(table);
				tableScroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				gbc = new GridBagConstraints();
				gbc.weightx = gbc.weighty = 1.0;
				gbc.fill = GridBagConstraints.BOTH;
				add(tableScroller,gbc);

				final JPanel progressPanel = new JPanel();
				progressPanel.setBorder(new TitledBorder("Progress on Experiment"));
				JProgressBar progressBar = new JProgressBar();
				progressPanel.add(new JButton(new AbstractAction("Start Experiment") {
						public void actionPerformed(ActionEvent ev) {
							final Blocker blocker = (Blocker)viewerContext.get("Blocker");
							final StringDistanceLearner learner = (StringDistanceLearner)viewerContext.get("Distance");
							final File datafile = (File)viewerContext.get("File");
							final MatchData data = new MatchData(datafile.getAbsolutePath());
							final Thread exptThread = new Thread() {
									public void run() {
										matchExpt = new MatchExpt(data,learner,blocker);
										matchExptViewer = matchExpt.toGUI();
										viewerContext.put("MatchExpt",matchExpt);
										viewerContext.put("ExptViewer",matchExptViewer);
										matchExptComplete = true;
										progressPanel.add(new JLabel("Experiment complete!")); 
										progressPanel.revalidate();
									}
								};
							exptThread.start();
						}
					}));
				ProgressCounter.setGraphicContext(new JProgressBar[]{progressBar});
				progressPanel.add(progressBar);
				gbc = new GridBagConstraints();
				gbc.weightx = 1.0; gbc.weighty = 0.5;
				gbc.gridy = 1;
				gbc.fill = GridBagConstraints.HORIZONTAL;
				add(progressPanel,gbc);
			}
			public boolean hasNext() { return true; }
			public boolean validateNext(java.util.List list) { 
				list.clear();
				list.add("You need to run the experiment and wait for it to finish before you can go to the next step.");
				return matchExptComplete;
			}
			public WizardPanel next() { return new ShowResults(viewerContext).getWizardPanel();	}
		}
		private class ResultWizardPanel extends NullWizardPanel {
			public ResultWizardPanel(Viewer v) {
				setBorder(new TitledBorder("SecondString: results"));
				add(v);
			}
		}

	}

	private static class ShowResults extends WizardViewer
	{
		public ShowResults(Map viewerContext) { super("Result",viewerContext); }
		public WizardPanel buildWizardPanel() {	
			return new ResultWizardPanel((Viewer)viewerContext.get("ExptViewer"),
																	 new SaveResults(viewerContext).getWizardPanel());	
		}
	}

	/** Display a viewer component in a WizardPanel */
	static private class ResultWizardPanel extends NullWizardPanel
	{
		WizardPanel nextPanel = null;
		public ResultWizardPanel(Viewer v,WizardPanel nextPanel) {
			this.nextPanel = nextPanel;
			setBorder(new TitledBorder("SecondString: results"));
			add(v);
		}
		public boolean hasNext() { return nextPanel!=null; }
		public boolean validateNext(java.util.List list) { return nextPanel!=null; }
		public WizardPanel next() { return nextPanel; }
	}

	/** Saves a serialized MatchExpt in the given file.
	 */
	private static class SaveResults extends WizardViewer
	{
		private boolean fileWasSaved = false;
		public SaveResults(Map viewerContext) { super("SavedResult",viewerContext); }
		public WizardPanel buildWizardPanel() {	return new SaveResultPanel(this); }
		private class SaveResultPanel extends NullWizardPanel 
		{
			public SaveResultPanel(Viewer enclosingViewer)
			{
				setBorder(new TitledBorder("SecondString: save results?"));
				add(new JLabel("Select a file to store these results in:"));
				final JTextField filePane = new JTextField(20);
				filePane.setEditable(false);
				add(filePane);
				final JFileChooser chooser = new JFileChooser();
				add(new JButton(new AbstractAction("Browse") {
						public void actionPerformed(ActionEvent ev) {
							int returnVal = chooser.showSaveDialog(null);
							if (returnVal==JFileChooser.APPROVE_OPTION) {
								File file = chooser.getSelectedFile();
								filePane.setText( file.getName() );
								Object evaluation = ((Viewer)viewerContext.get("ExptViewer")).getContent();
								try {
									IOUtil.saveSerialized( (Serializable)evaluation, file );
									fileWasSaved = true;
								} catch (IOException ex) {
									System.err.println("error saving: "+ex);
								}
							}
						}
					}));
			}
			public boolean canFinish() { return true; }
			public boolean validateFinish(java.util.List list) { 
				list.add("You haven't saved the results. Click cancel to exit without saving them");
				return fileWasSaved; 
			}
		}
	}
}

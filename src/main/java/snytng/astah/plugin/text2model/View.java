package snytng.astah.plugin.text2model;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.StringJoiner;
import java.util.Vector;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;

import com.change_vision.jude.api.inf.AstahAPI;
import com.change_vision.jude.api.inf.model.IActivityDiagram;
import com.change_vision.jude.api.inf.model.IClassDiagram;
import com.change_vision.jude.api.inf.model.IDiagram;
import com.change_vision.jude.api.inf.model.IStateMachineDiagram;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.IPresentation;
import com.change_vision.jude.api.inf.project.ProjectAccessor;
import com.change_vision.jude.api.inf.project.ProjectEvent;
import com.change_vision.jude.api.inf.project.ProjectEventListener;
import com.change_vision.jude.api.inf.ui.IPluginExtraTabView;
import com.change_vision.jude.api.inf.ui.ISelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionEvent;
import com.change_vision.jude.api.inf.view.IDiagramEditorSelectionListener;
import com.change_vision.jude.api.inf.view.IDiagramViewManager;
import com.change_vision.jude.api.inf.view.IEntitySelectionEvent;
import com.change_vision.jude.api.inf.view.IEntitySelectionListener;

public class View 
extends 
JPanel
implements 
IPluginExtraTabView, 
ProjectEventListener, 
IEntitySelectionListener, 
IDiagramEditorSelectionListener
{
	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(View.class.getName());
	static {
		ConsoleHandler consoleHandler = new ConsoleHandler();
		consoleHandler.setLevel(Level.CONFIG);      
		logger.addHandler(consoleHandler);
		logger.setUseParentHandlers(false);
	}

	/**
	 * プロパティファイルの配置場所
	 */
	private static final String VIEW_PROPERTIES = "snytng.astah.plugin.text2model.view";

	/**
	 * リソースバンドル
	 */
	private static final ResourceBundle VIEW_BUNDLE = ResourceBundle.getBundle(VIEW_PROPERTIES, Locale.getDefault());

	private String title = "<Text2Model>";
	private String description = "<This plugin creates a diagram from text.>";

	private static final long serialVersionUID = 1L;
	private transient ProjectAccessor projectAccessor = null;
	private transient IDiagramViewManager diagramViewManager = null; 

	public View() {
		try {
			projectAccessor = AstahAPI.getAstahAPI().getProjectAccessor();
			diagramViewManager = projectAccessor.getViewManager().getDiagramViewManager();
		} catch (Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}

		initProperties();

		initComponents();
	}

	private void initProperties() {
		try {
			title = VIEW_BUNDLE.getString("pluginExtraTabView.title");
			description = VIEW_BUNDLE.getString("pluginExtraTabView.description");
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}
	
	private DiagramWriter getDiagramWriter(IDiagram d){
		DiagramWriter dw = null;
		
		if(d instanceof IClassDiagram){
			dw = ClassDiagramWriter.getInstance();
		} 
		else 
		if(d instanceof IActivityDiagram){
			dw = ActivityDiagramWriter.getInstance(); 
		}
		else 
		if(d instanceof IUseCaseDiagram){
			dw = UseCaseDiagramWriter.getInstance(); 
		}
		else 
		if(d instanceof IStateMachineDiagram){
			dw = StateMachineDiagramWriter.getInstance(); 
		}

		return dw;
	}

	private DiagramMessages getDiagramMessages(IDiagram d){
		DiagramMessages dm = null;

		if(d instanceof IClassDiagram){
			dm = ClassDiagramMessages.getInstance();
		}
		else 
		if(d instanceof IUseCaseDiagram){
			dm = UseCaseDiagramMessages.getInstance(); 
		}
		else 
		if(d instanceof IStateMachineDiagram){
			dm = StateMachineDiagramMessages.getInstance(); 
		}
		return dm;
	}

	private void initComponents() {
		// レイアウトの設定
		setLayout(new GridLayout(1,2));
		add(createTablePane());
		add(createTextAreaPane());
	}

	private void addDiagramListeners(){
		diagramViewManager.addDiagramEditorSelectionListener(this);
		diagramViewManager.addEntitySelectionListener(this);
	}

	private void removeDiagramListeners(){
		diagramViewManager.removeDiagramEditorSelectionListener(this);
		diagramViewManager.removeEntitySelectionListener(this);
	}


	private JTextArea textArea = new JTextArea();
	private JToggleButton checkBoxNoun = null;
	private JCheckBox checkBoxAddAttribute = null;
	private JCheckBox checkBoxAddOperation = null;
	private JCheckBox checkBoxCreateNewSequenceDiagram = null;
	private JLabel modeLabel = new JLabel();

	@SuppressWarnings("serial")
	private Container createTextAreaPane(){

		textArea.addKeyListener(new KeyAdapter() {
			@Override
			public void keyReleased(KeyEvent e) {
				int keycode = e.getKeyCode();
				int mod = e.getModifiersEx();
				String msg = "keycode:mod=" + keycode + ":" + mod;
				logger.log(Level.INFO,msg);

				// Enterが押されていなければ無反応
				if(keycode != KeyEvent.VK_ENTER){
					return;
				}

				// テキスト解析
				analyzeTextAndSetReqTable();

				// Ctrl-Enterの場合には全部追加
				if((mod & InputEvent.CTRL_DOWN_MASK) != 0){
					addAllFunctionsFromReqTableThread();
				}				

			}
		});
		textArea.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				analyzeTextAndSetReqTable();
			}

		});

		JPanel optionPanel = new JPanel();

		checkBoxNoun = new JToggleButton("名詞モードOFF");
		checkBoxNoun.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				if(checkBoxNoun.isSelected()){
					checkBoxNoun.setText("名詞モードON");
				} else {
					checkBoxNoun.setText("名詞モードOFF");					
				}
				super.mouseReleased(e);
				analyzeTextAndSetReqTable();
			}
		});
		optionPanel.add(checkBoxNoun);

		optionPanel.add(new JSeparator(SwingConstants.VERTICAL){
			@Override public Dimension getPreferredSize() {
				return new Dimension(1, 16);
			}
			@Override public Dimension getMaximumSize() {
				return this.getPreferredSize();
			}
		});

		checkBoxAddAttribute = new JCheckBox("属性追加");
		checkBoxAddAttribute.addActionListener(e -> analyzeTextAndSetReqTable());		
		checkBoxAddOperation = new JCheckBox("操作追加");
		checkBoxAddOperation.addActionListener(e -> {
			checkBoxCreateNewSequenceDiagram.setEnabled(checkBoxAddOperation.isSelected());
			analyzeTextAndSetReqTable();
			});		
		checkBoxCreateNewSequenceDiagram = new JCheckBox("シーケンス図作成");
		checkBoxCreateNewSequenceDiagram.setEnabled(checkBoxAddOperation.isSelected());
		checkBoxCreateNewSequenceDiagram.addActionListener(e -> analyzeTextAndSetReqTable());		

		optionPanel.add(checkBoxAddAttribute);
		optionPanel.add(checkBoxAddOperation);	
		optionPanel.add(checkBoxCreateNewSequenceDiagram);	

		Font modeLabelFont = modeLabel.getFont().deriveFont(Font.BOLD);
		modeLabel.setFont(modeLabelFont);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		p.add(modeLabel, BorderLayout.NORTH);
		p.add(new JScrollPane(textArea), BorderLayout.CENTER);
		p.add(optionPanel, BorderLayout.SOUTH);

		return p;
	}

	private void analyzeTextAndSetReqTable() {
		String str = textArea.getText();
		logger.log(Level.INFO, () -> "textArea.text=" + str);

		// 今選択している図のタイプを取得する
		IDiagram diagram = diagramViewManager.getCurrentDiagram();

		// 主語、目的語、述語に分ける
		String[][] sovs = new String[][]{}; 

		DiagramWriter dw = getDiagramWriter(diagram);
		
		if(dw != null){
			dw.setNounMode(checkBoxNoun.isSelected());
			Function<String, String[][]> textAnalizer = dw.getTextAanalyzer();
			sovs = textAnalizer.apply(str);			
		}

		// メッセージの表示
		reqTableModel.setRowCount(0); // 表示内容を消去

		// 新しいエントリをreqTableModelへ追加
		for(String[] sov : sovs){
			String sov0  = sov[0] != null ? sov[0] : ""; 
			String sov0a = sov[0] != null ? "は、" : ""; 
			String sov1  = sov[1] != null ? sov[1] : ""; 
			String sov1a = sov[2] != null ? "の"   : ""; 
			String sov2  = sov[2] != null ? sov[2] : ""; 
			String sov3  = sov[3] != null ? sov[3] : ""; 
			reqTableModel.addRow(new String[]{PLUS_MARK, sov0, sov0a, sov1, sov1a, sov2, sov3});						
		}
	}


	private JTable reqTable = null;
	private String[] reqTableTitle = new String[]{"○/×", "主語","は、","目的語","の","属性","～する","文章"};
	private DefaultTableModel reqTableModel = new DefaultTableModel(reqTableTitle, 0);


	private static final String PLUS_MARK = "＋";
	JButton addAllButton = new JButton("Add All " + PLUS_MARK);
	JButton replaceReadTextButton = new JButton("Replace ==>");
	JProgressBar progressBar = new JProgressBar();
	int progressBarMaxCount = 0;
	int progressBarCount = 0;

	private void addAllFunctionsFromReqTableThread(){
		// 全関数を追加するときには、シーケンス図を新規に作成する
		SequenceDiagramWriter.getInstance().setCreateNewSequenceDiagram(true);
		// 全機能を追加するときには、アクティビティ図は新規に作成する
		ActivityDiagramWriter.getInstance().setCreateNewActivityDiagram(true);

		new Thread(()->{
			@SuppressWarnings("unchecked")
			Vector<Vector<String>> dataTable = (Vector<Vector<String>>) reqTableModel.getDataVector().clone();
			int rowCount = dataTable.size();

			progressBar.setMaximum(rowCount);
			progressBar.setStringPainted(true);
			progressBar.setVisible(true);

			logger.log(Level.INFO,"addAllFunctionsFromReqTableThread thread");

			int col = 0;
			for(int row = 0; row < rowCount; row++){
				addFunctionFromDataTable(dataTable, row, col);
				progressBar.setValue(row+1);
			}

			progressBar.setVisible(false);

			updateDiagramView();
		}).start();
	}

	private Container createTablePane() {
		reqTable = new JTable(reqTableModel);
		reqTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
		reqTable.setSurrendersFocusOnKeystroke(true); // 選択したら編集できるようにする

		reqTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				logger.log(Level.INFO,"reqTable mouselistener");
				int col = reqTable.getSelectedColumn();
				int row = reqTable.getSelectedRow();

				@SuppressWarnings("unchecked")
				Vector<Vector<String>> dataTable = (Vector<Vector<String>>) reqTableModel.getDataVector().clone();

				addFunctionFromDataTable(dataTable, row, col);
				updateDiagramView();
			}
		});

		progressBar.setVisible(false);

		addAllButton.addActionListener(e -> addAllFunctionsFromReqTableThread());
		replaceReadTextButton.addActionListener(e -> replaceReadText());

		JPanel panel = new JPanel();

		panel.setLayout(new BorderLayout());
		panel.add(new JScrollPane(reqTable), BorderLayout.CENTER);

		JPanel addPanel = new JPanel();
		addPanel.setLayout(new BorderLayout());
		addPanel.add(addAllButton, BorderLayout.WEST);
		addPanel.add(progressBar, BorderLayout.CENTER);
		addPanel.add(replaceReadTextButton, BorderLayout.EAST);

		panel.add(addPanel, BorderLayout.SOUTH);

		return panel;
	}

	private void addFunctionFromDataTable(Vector<Vector<String>> dataTable, int row, int col) {
		// 選択セルの値が追加記号だったらモデルに追加
		String cellstr = dataTable.elementAt(row).elementAt(col); 

		if(cellstr.equals(PLUS_MARK)){
			logger.log(Level.INFO, "Add this row data to diagram");

			String subjectName   = dataTable.elementAt(row).elementAt(1);
			String objectName    = dataTable.elementAt(row).elementAt(3); 
			String attributeName = dataTable.elementAt(row).elementAt(5);
			String relationName  = dataTable.elementAt(row).elementAt(6); 

			addFunction(subjectName, objectName, attributeName, relationName);
		}
	}

	private void addFunction(String subjectName, String objectName, String attributeName, String relationName) {
		IDiagram diagram = diagramViewManager.getCurrentDiagram();
		if(diagram == null){
			return;
		}

		try {
			// 機能追加クラスを初期化
			FunctionCreator f = new FunctionCreator(projectAccessor, diagram, checkBoxAddAttribute.isSelected(), checkBoxAddOperation.isSelected());
			f.subjectName   = subjectName;
			f.objectName    = objectName;
			f.attributeName = attributeName;
			f.relationName  = relationName;

			DiagramWriter dw = getDiagramWriter(diagram);

			if(dw != null){
				dw.setSequenceDiagramMode(checkBoxCreateNewSequenceDiagram.isEnabled() && checkBoxCreateNewSequenceDiagram.isSelected());
				Consumer<FunctionCreator> functionConsumer = dw.getFunctionVisualizer(); 
				functionConsumer.accept(f);				
			}
			else {
				logger.log(Level.WARNING, () -> "The current diagram type is NOT supported.");
			}
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	/**
	 * プロジェクトが変更されたら表示を更新する
	 */
	@Override
	public void projectChanged(ProjectEvent e) {
		updateDiagramView();
	}
	@Override
	public void projectClosed(ProjectEvent e) {
		// Do nothing when project is closed
	}

	@Override
	public void projectOpened(ProjectEvent e) {
		// Do nothing when project is opened
	}

	/**
	 * 図の選択が変更されたら表示を更新する
	 */
	@Override
	public void diagramSelectionChanged(IDiagramEditorSelectionEvent e) {
		updateDiagramView();
	}

	/**
	 * 要素の選択が変更されたら表示を更新する
	 */
	@Override
	public void entitySelectionChanged(IEntitySelectionEvent e) {
		updateDiagramView();
	}

	/**
	 * 表示を更新する
	 */
	private void updateDiagramView() {
		try {
			// 今選択している図のタイプを取得する
			IDiagram diagram = diagramViewManager.getCurrentDiagram();

			// モード表示を変更
			DiagramWriter dw = getDiagramWriter(diagram);
			if(dw != null){
				Supplier<String> modeGetter = dw.getModeGetter();
				modeLabel.setText(modeGetter.get());				
			}
			else {
				modeLabel.setText("未対応");
			}
			
			
			DiagramMessages dm = getDiagramMessages(diagram);
			
			if(dm != null){
				// メッセージのリスト化
				List<String> messages = dm.getMessages(
						diagram, 
						Arrays.stream(diagramViewManager.getSelectedPresentations())
						.map(IPresentation::getModel)
						);

				// メッセージの表示
				reqTableModel.setRowCount(0); // 表示内容を消去

				for(String message : messages){
					String[] words = message.split(",");
					String sentence = String.join("", words);
					if(words.length == 5){
						reqTableModel.addRow(new String[]{words[0], words[1], words[2], words[3], "", "", words[4], sentence});
					} else {
						String msg = "words format error:" + message;
						logger.log(Level.WARNING,msg);
					}
				}

				// 幅の調整
				reqTable.getColumn(reqTableTitle[0]).setPreferredWidth(50);
				reqTable.getColumn(reqTableTitle[2]).setPreferredWidth(40);
				reqTable.getColumn(reqTableTitle[4]).setPreferredWidth(30);
				reqTable.getColumn(reqTableTitle[6]).setPreferredWidth(60);
				reqTable.getColumn(reqTableTitle[7]).setPreferredWidth(500);
			}
			
		}catch(Exception e){
			logger.log(Level.WARNING, e.getMessage(), e);
		}
	}

	private void replaceReadText(){
		StringJoiner sj = new StringJoiner(System.lineSeparator());
		@SuppressWarnings("unchecked")
		Vector<Vector<String>> dataVector = reqTableModel.getDataVector(); 
		for(Vector<String> vs : dataVector){
			sj.add(vs.elementAt(7).substring(1)); // 先頭の○×を取り除く
		}
		textArea.setText(sj.toString());
	}

	// IPluginExtraTabView
	@Override
	public void addSelectionListener(ISelectionListener listener) {
		// Do nothing
	}

	@Override
	public void activated() {
		// リスナーへの登録
		addDiagramListeners();
	}

	@Override
	public void deactivated() {
		// リスナーへの登録
		removeDiagramListeners();
	}

	@Override
	public Component getComponent() {
		return this;
	}

	@Override
	public String getDescription() {
		return description;
	}

	@Override
	public String getTitle() {
		return title;
	}

}

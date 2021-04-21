package snytng.astah.plugin.text2model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.change_vision.jude.api.inf.exception.InvalidUsingException;
import com.change_vision.jude.api.inf.model.IAssociation;
import com.change_vision.jude.api.inf.model.IAttribute;
import com.change_vision.jude.api.inf.model.IClass;
import com.change_vision.jude.api.inf.model.IElement;
import com.change_vision.jude.api.inf.model.IExtend;
import com.change_vision.jude.api.inf.model.IInclude;
import com.change_vision.jude.api.inf.model.IUseCase;
import com.change_vision.jude.api.inf.model.IUseCaseDiagram;
import com.change_vision.jude.api.inf.presentation.IPresentation;

public class UseCaseDiagramReader {
	
	/**
	 * logger
	 */
	static final Logger logger = Logger.getLogger(UseCaseDiagramReader.class.getName());
	static {
	      ConsoleHandler consoleHandler = new ConsoleHandler();
	      consoleHandler.setLevel(Level.CONFIG);      
	      logger.addHandler(consoleHandler);
	      logger.setUseParentHandlers(false);
	}

	private IUseCaseDiagram diagram = null;

	public UseCaseDiagramReader(IUseCaseDiagram diagram){
		this.diagram = diagram;
	}

	/**
	 * ユースケース図に含まれるユースケースの数を取得する
	 * @return ユースケース数
	 */
	public int getUseCases(){
		IUseCase[] nodes = null;
		try {
			nodes = Arrays.stream(this.diagram.getPresentations())
					.map(IPresentation::getModel)
					.filter(IUseCase.class::isInstance)
					.toArray(IUseCase[]::new);
		} catch (InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		return nodes == null ? 0 : nodes.length;
	}

	public IPresentation[] supportedPresentation(IPresentation[] ps){
		return Arrays.stream(ps)
				.filter(p -> isSupportedElement(p.getModel()))
				.toArray(IPresentation[]::new);
	}

	public IPresentation[] unsupportedPresentation(IPresentation[] ps){
		return Arrays.stream(ps)
				.filter(p -> isSupportedElement(p.getModel()))
				.toArray(IPresentation[]::new);
	}
	
	public boolean isSupportedElement(IElement e){
		return 	read(e) != null;
	}

	public String read(IElement e){
		String message = "○,";

		// 関連を読み上げ
		if(e instanceof IAssociation) {
			IAssociation as = (IAssociation)e;
			IAttribute[] ats = as.getMemberEnds();
			if(ats[0].getType() instanceof IUseCase){
				message += ats[1].getType().getName() + ",は、,," + ats[0].getType().getName();				
			} else if(ats[1].getType() instanceof IUseCase){
				message += ats[0].getType().getName() + ",は、,," + ats[1].getType().getName();
			} else {
				message = null;
			}

		}
		// <<include>>を読み上げ
		else if(e instanceof IInclude){
			IInclude in = (IInclude)e;
			StringBuilder actors = new StringBuilder();
			getActors(in.getIncludingCase(), new HashSet<>(), actors);
			message += actors + ",は、,," + in.getIncludingCase().getName() + "とき、いつも" + in.getAddition().getName();
		}
		// <<extend>>を読み上げ
		else if(e instanceof IExtend){
			IExtend ex = (IExtend)e;
			StringBuilder actors = new StringBuilder();
			getActors(ex.getExtendedCase(), new HashSet<>(), actors);
			message += actors + ",は、,," + ex.getExtendedCase().getName() + "とき、" + ex.getExtension().getName();
		}
		// それ以外は未サポート
		else {
			message = null;
		}

		return message;
	}

	private void getActors(IUseCase uc, Set<IUseCase> ucs, StringBuilder actors) {

		//System.out.println("getActors: <--" + uc.getName());
		if(! ucs.contains(uc)){
			ucs.add(uc);
		}

		while(true){

			int count = 0;

			IInclude[] ins = uc.getAdditionInvs();
			for(IInclude in : ins){
				IUseCase u = in.getIncludingCase();
				//System.out.println("getActors: <<include>> " + in);

				if(! ucs.contains(u)){
					ucs.add(u);
					getActors(u, ucs, actors);
					count++;
				}
			}

			IExtend[] exs = uc.getExtends();
			for(IExtend ex : exs){
				IUseCase u = ex.getExtendedCase();
				//System.out.println("getActors: <<extend>> " + ex);
				if(! ucs.contains(u)){
					ucs.add(u);
					getActors(u, ucs, actors);
					count++;
				}
			}

			IAttribute[] ats = uc.getAttributes();
			for(IAttribute at : ats){
				//System.out.println("getActors: <<attribute>> " + at);
				if(! (at.getType() instanceof IUseCase)){
					IClass c = at.getType();
					try {
						IPresentation[] pind = diagram.getPresentations();
						IPresentation[] pofc = c.getPresentations();

						if(Arrays.stream(pind)
								.filter(p -> Arrays.asList(pofc).contains(p))
								.count() > 0){
							if(! actors.toString().isEmpty()){
								actors.append(", ");
							}
							actors.append(at.getType().getName());
							//System.out.println("getActors: append" + at.getType().getName());
						}

					} catch (InvalidUsingException e) {
						logger.log(Level.WARNING, e.getMessage(), e);
					}						
				}
			}

			if(count == 0){
				break;
			}
		}

		//System.out.println("getActors: -->" + uc.getName());

	}

	public List<String> read(){
		UseCaseDiagramReader ucdr = new UseCaseDiagramReader(diagram);

		List<String> lines = new ArrayList<>();
		
		try {
			IPresentation[] ps = diagram.getPresentations();

			for(IPresentation p : ucdr.supportedPresentation(ps)){
				String m = ucdr.read(p.getModel());
				if(m != null && ! m.isEmpty()){
					lines.add(m);
				}
			}

		} catch (InvalidUsingException e) {
			logger.log(Level.WARNING, e.getMessage(), e);
		}
		return lines;
	}


}

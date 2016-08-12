package org.ekstep.language.wordchian;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.ekstep.language.common.enums.LanguageErrorCodes;
import org.ekstep.language.common.enums.LanguageObjectTypes;
import org.ekstep.language.common.enums.LanguageParams;
import org.ekstep.language.measures.entity.WordComplexity;
import org.ekstep.language.util.WordUtil;

import com.ilimi.common.dto.Response;
import com.ilimi.common.dto.ResponseParams;
import com.ilimi.common.dto.ResponseParams.StatusType;
import com.ilimi.common.exception.ServerException;
import com.ilimi.graph.dac.enums.RelationTypes;
import com.ilimi.graph.dac.enums.SystemProperties;
import com.ilimi.graph.dac.model.Node;
import com.ilimi.graph.dac.model.Relation;

public class WordChainUtil {
	
	private WordUtil wordUtil = new WordUtil();
	
		
	public void updateWordSet(String languageId, Node node, WordComplexity wc) throws Exception{
		String status = (String) node.getMetadata().get(LanguageParams.status.name());
		if(status == null)
			return;
		if("Live".equalsIgnoreCase(status)){
			new RhymingSoundSet(languageId, node, wc).create();
			new PhoneticBoundarySet(languageId, node, wc).create();
			Node updatedNode = wordUtil.getDataNode(languageId, node.getIdentifier());
			node.setInRelations(updatedNode.getInRelations());			
		}else{
			List<Relation> inRelation = node.getInRelations();
			if(inRelation != null){
				Iterator<Relation> irItr =inRelation.iterator();
				while(irItr.hasNext()){
					Relation rel = irItr.next();
					if(rel.getRelationType().equalsIgnoreCase(RelationTypes.SET_MEMBERSHIP.relationName())&&
							StringUtils.equalsIgnoreCase(
									rel.getStartNodeMetadata().get(SystemProperties.IL_FUNC_OBJECT_TYPE.name()).toString(),
		                            LanguageObjectTypes.WordSet.name())){
						irItr.remove();
					}
				}
				node.setInRelations(inRelation);
				
				Response wordResponse = wordUtil.updateWord(node, languageId, node.getIdentifier());
				if (checkError(wordResponse)) {
					throw new ServerException(LanguageErrorCodes.ERR_UPDATE_WORD.name(),
							getErrorMessage(wordResponse));
				}
			}
		}

	}
	
    protected boolean checkError(Response response) {
        ResponseParams params = response.getParams();
        if (null != params) {
            if (StringUtils.equals(StatusType.failed.name(), params.getStatus())) {
                return true;
            }
        }
        return false;
    }

    protected String getErrorMessage(Response response) {
        ResponseParams params = response.getParams();
        if (null != params) {
            return params.getErrmsg();
        }
        return null;
    }

}

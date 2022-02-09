package com.devepos.adt.cst.ui.internal.codesearch.result;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.IEditorMatchAdapter;
import org.eclipse.search.ui.text.IFileMatchAdapter;
import org.eclipse.search.ui.text.Match;

import com.devepos.adt.base.ui.AdtBaseUIResources;
import com.devepos.adt.base.ui.IAdtBaseStrings;
import com.devepos.adt.base.ui.tree.ICollectionTreeNode;
import com.devepos.adt.base.ui.tree.ITreeNode;
import com.devepos.adt.base.ui.tree.PackageNode;
import com.devepos.adt.cst.model.codesearch.ICodeSearchResult;
import com.devepos.adt.cst.ui.internal.CodeSearchUIPlugin;
import com.devepos.adt.cst.ui.internal.IImages;
import com.devepos.adt.cst.ui.internal.codesearch.CodeSearchQuery;

/**
 * Represents the result of an ABAP Code Search
 *
 * @author Ludwig Stockbauer-Muhr
 *
 */
public class CodeSearchResult extends AbstractTextSearchResult {

  private IEditorMatchAdapter editorMatchAdapter;
  private List<ITreeNode> flatResult = new ArrayList<>();
  private FileMatchesCache fileMatchesCache = new FileMatchesCache();
  private ResultTreeBuilder resultTree;
  private CodeSearchQuery searchQuery;
  private ICollectionTreeNode searchResultRootNode;
  private int resultCount;
  private boolean noObjectsInScope;

  public CodeSearchResult(final CodeSearchQuery searchQuery) {
    this.searchQuery = searchQuery;
  }

  /**
   * Take query result and convert it into a flat and tree like result for the
   * search view
   */
  public void addResult(final ICodeSearchResult result) {
    if (result == null) {
      return;
    }
    logMessages(result);
    if (result.getNumberOfResults() <= 0) {
      return;
    }

    resultCount += result.getNumberOfResults();

    if (resultTree == null) {
      resultTree = new ResultTreeBuilder(fileMatchesCache, searchQuery.getProjectProvider()
          .getDestinationId());
    }
    if (searchResultRootNode == null) {
      searchResultRootNode = resultTree.getRootNode();
    }
    resultTree.addResultToTree(result);
    List<ITreeNode> currentFlatResult = resultTree.getFlatResult();
    if (currentFlatResult != null) {
      flatResult.addAll(currentFlatResult);
      for (ITreeNode matchNode : currentFlatResult) {
        addMatch(new Match(matchNode, -1, -1));
      }
    }
    resultTree.clearBuffersOfLastResult();
  }

  @Override
  public IEditorMatchAdapter getEditorMatchAdapter() {
    if (editorMatchAdapter == null) {
      editorMatchAdapter = new CodeSearchEditorMatcher(this, searchQuery.getProjectProvider()
          .getProject());
    }
    return editorMatchAdapter;
  }

  @Override
  public IFileMatchAdapter getFileMatchAdapter() {
    return null;
  }

  public List<ITreeNode> getFlatResult() {
    return flatResult;
  }

  @Override
  public ImageDescriptor getImageDescriptor() {
    return CodeSearchUIPlugin.getDefault().getImageDescriptor(IImages.CODE_SEARCH);
  }

  @Override
  public String getLabel() {
    if (searchQuery == null) {
      return "ABAP Code Search result";
    }
    String resultsLabel = null;
    if (noObjectsInScope) {
      resultsLabel = "No Objects in scope";
    } else {
      if (resultCount == 1) {
        resultsLabel = AdtBaseUIResources.getString(IAdtBaseStrings.SearchUI_OneResult_xmsg);
      } else if (resultCount > 1) {
        resultsLabel = AdtBaseUIResources.format(IAdtBaseStrings.SearchUI_SpecificResults_xmsg,
            resultCount);
      } else {
        resultsLabel = AdtBaseUIResources.getString(IAdtBaseStrings.SearchUI_NoResults_xmsg);
      }
    }
    return String.format("%s %s - %s", "Code Matches for", searchQuery.getQuerySpecs(),
        resultsLabel);
  }

  public Set<SearchMatchNode> getMatchNodesForFileUri(final String fileUri) {
    return fileMatchesCache.getNodes(fileUri);
  }

  @Override
  public ISearchQuery getQuery() {
    return searchQuery;
  }

  /**
   * Retrieves the root node of the result tree
   *
   * @return the root node of the result tree
   */
  public ICollectionTreeNode getResultTree() {
    return searchResultRootNode;
  }

  @Override
  public String getTooltip() {
    return searchQuery != null ? searchQuery.getQuerySpecs().getQuery(false) : null;
  }

  @Override
  public void removeAll() {
    if (flatResult != null) {
      flatResult.clear();
    }
    resultCount = 0;
    fileMatchesCache.clear();
    if (searchResultRootNode != null) {
      searchResultRootNode.removeAllChildren();
    }
    if (resultTree != null) {
      resultTree.clearPackageNodeCache();
    }
    searchResultRootNode = null;
    super.removeAll();
  }

  /**
   * Removes the given <code>child</code> node of the given <code>parent</code> node
   *
   * @param parent collection tree node
   * @param child  child node that should be removed
   */
  public void removeChildeNode(final ICollectionTreeNode parent, final ITreeNode child) {
    if (child instanceof PackageNode) {
      resultTree.removePackageNode((PackageNode) child);
    }
    parent.removeChild(child);
  }

  /**
   * Removes the given match result node from the tree
   *
   * @param match the match node to be removed
   */
  public void removeSearchResult(final ITreeNode match) {
    resultCount--;
    if (flatResult != null) {
      flatResult.remove(match);
    }
    if (match instanceof SearchMatchNode) {
      fileMatchesCache.removeNode((SearchMatchNode) match);
    }
  }

  /**
   * Resets the result to be ready for a new search
   */
  public void reset() {
    removeAll();
    noObjectsInScope = false;
  }

  /**
   * Set flag to indicate that no objects were found that could be searched
   */
  public void setNoObjectsInScope() {
    noObjectsInScope = true;
  }

  private void logMessages(final ICodeSearchResult result) {
    if (result.getResponseMessageList() != null) {
      IStatus searchStatus = result.getResponseMessageList()
          .toStatus(CodeSearchUIPlugin.PLUGIN_ID, "Problems occurred during ABAP Code search");
      if (searchStatus != null) {
        CodeSearchUIPlugin.getDefault().getLog().log(searchStatus);
      }
    }
  }

}
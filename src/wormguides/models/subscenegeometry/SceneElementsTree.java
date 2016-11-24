/*
 * Bao Lab 2016
 */

/*
 * Bao Lab 2016
 */

package wormguides.models.subscenegeometry;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Queue;

import javafx.scene.control.TreeItem;

/**
 * Tree to represent heirarchy of structures as specified by CellShapesConfig.csv
 */
public class SceneElementsTree {

    /** Root node of the scene elements tree. This is not shown in the UI treeview. */
    private final TreeItem<StructureTreeNode> root;

    /**
     * Constructor
     */
    public SceneElementsTree() {
        root = new TreeItem<>(new StructureTreeNode(true, "root"));
    }

    /**
     * Deselects all structure tree nodes
     */
    public void deselectAllNodes() {
        final Queue<TreeItem<StructureTreeNode>> treeItems = new LinkedList<>();
        treeItems.add(root);
        TreeItem<StructureTreeNode> pointer;
        do {
            pointer = treeItems.remove();
            treeItems.addAll(pointer.getChildren());
        } while (!treeItems.isEmpty());
    }

    /**
     * @return root of the structure tree
     */
    public TreeItem<StructureTreeNode> getRoot() {
        return root;
    }

    /**
     * Adds a structure (by scene name) to the structure tree under a specified category. If no category is specified
     * (it is "root", an empty string, or null), then the structure is added to the top level root category. If the
     * category does not exist, it is created under the top level category.
     *
     * @param sceneName
     *         the structure's scene name to add
     * @param category
     *         the catogory to add the structure to
     */
    public void addStructure(final String sceneName, String category) {
        if (sceneName != null) {
            if (category == null || category.isEmpty()) {
                category = "root";
            }
            // breadth-first-search to find the category
            final Queue<TreeItem<StructureTreeNode>> treeItems = new LinkedList<>();
            treeItems.add(root);
            TreeItem<StructureTreeNode> pointer;
            StructureTreeNode tempNode;
            do {
                pointer = treeItems.remove();
                tempNode = pointer.getValue();
                if (tempNode.isCategoryNode() && tempNode.getNodeText().equalsIgnoreCase(category)) {
                    pointer.getChildren().add(new TreeItem<>(new StructureTreeNode(false, sceneName)));
                    break;
                }
                treeItems.addAll(pointer.getChildren());
            } while (!treeItems.isEmpty());
        }
    }

    /**
     * Adds a category to the specified parent category. To add a category to the top level "root" category, pass
     * "root", an empty string, or null as the parent parameter.
     *
     * @param parentCategory
     *         the parent to which the category is added
     * @param category
     *         the category to add
     */
    public void addCategory(String parentCategory, final String category) {
        if (category != null) {
            if (parentCategory == null || parentCategory.isEmpty()) {
                // add category to root
                parentCategory = "root";
            }
            // add category to specified parent if the parent exists
            // use breadth-first-search to find the parent tree item
            final Queue<TreeItem<StructureTreeNode>> treeItems = new LinkedList<>();
            TreeItem<StructureTreeNode> pointer = root;
            StructureTreeNode tempNode;
            do {
                treeItems.add(pointer);
                treeItems.addAll(pointer.getChildren());
                pointer = treeItems.remove();
                tempNode = pointer.getValue();
                if (tempNode.isCategoryNode() && tempNode.getNodeText().equalsIgnoreCase(parentCategory)) {
                    final TreeItem<StructureTreeNode> newCategoryNode = new TreeItem<>(new StructureTreeNode(
                            true,
                            category));
                    pointer.getChildren().add(newCategoryNode);
                    break;
                }
            } while (!treeItems.isEmpty());
        }
    }

    /**
     * @param category
     *         the queried category
     *
     * @return the parent of the category, "root" if the category does not exist in the tree
     */
    public String getParentCategory(final String category) {
        String parent = "root";
        if (category != null) {
            final Queue<TreeItem<StructureTreeNode>> treeItems = new LinkedList<>();
            treeItems.add(root);

            TreeItem<StructureTreeNode> pointer;
            StructureTreeNode tempNode;
            Collection<TreeItem<StructureTreeNode>> children;
            do {
                pointer = treeItems.remove();
                children = pointer.getChildren();
                for (TreeItem<StructureTreeNode> child : children) {
                    tempNode = child.getValue();
                    if (tempNode.isCategoryNode() && tempNode.getNodeText().equalsIgnoreCase(category)) {
                        return pointer.getValue().getNodeText();
                    }
                }
                treeItems.addAll(children);
            } while (!treeItems.isEmpty());
        }
        return parent;
    }

    /**
     * @param category
     *         the category to check
     *
     * @return true if the structure tree contains that category, false otherwise
     */
    public boolean containsCategory(String category) {
        if (category != null) {
            category = category.trim();
            // bread-first-search for category
            final Queue<TreeItem<StructureTreeNode>> treeItems = new LinkedList<>();
            treeItems.add(root);

            TreeItem<StructureTreeNode> pointer;
            StructureTreeNode tempNode;
            Collection<TreeItem<StructureTreeNode>> children;
            do {
                pointer = treeItems.remove();
                children = pointer.getChildren();
                for (TreeItem<StructureTreeNode> child : children) {
                    tempNode = child.getValue();
                    if (tempNode.isCategoryNode() && tempNode.getNodeText().equalsIgnoreCase(category)) {
                        return true;
                    }
                }
                treeItems.addAll(children);
            } while (!treeItems.isEmpty());
        }
        return false;
    }
}

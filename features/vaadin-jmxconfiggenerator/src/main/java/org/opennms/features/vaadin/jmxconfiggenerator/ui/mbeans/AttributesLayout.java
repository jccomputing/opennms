package org.opennms.features.vaadin.jmxconfiggenerator.ui.mbeans;

import com.vaadin.ui.VerticalLayout;

class AttributesLayout extends VerticalLayout {

    private  AttributesTable attributesTable;
//    private final EditControls footer;
    private final MBeansController controller;

    AttributesLayout(MBeansController controller) {
        this.controller = controller;
//        attributesTable = new AttributesTable(this.controller, new MBeansController.Callback() {
//            @Override
//            public Container getContainer() {
//                return AttributesLayout.this.controller.getAttributeContainer(AttributesLayout.this.controller.getSelectedMBean());
//            }
//        });
//        footer = new EditControls(this.attributesTable);
        setSizeFull();
//        addComponent(footer);
        addComponent(attributesTable);
        setSpacing(false);
        setMargin(false);
//        addFooterHooks();
        setExpandRatio(attributesTable, 1);
    }

//    public void callback(EditControls.ButtonType type, Table outer) {
//        if (type == EditControls.ButtonType.cancel) {
//            outer.discard();
////            controller.fireViewStateChanged(ViewState.LeafSelected, outer);
//        }
//        if (type == EditControls.ButtonType.edit) {
////            controller.fireViewStateChanged(ViewState.Edit, outer);
//        }
//		if (type == EditControls.ButtonType.save) {
//            if (outer.isValid()) {
//                outer.commit();
////                controller.fireViewStateChanged(ViewState.LeafSelected, outer);
//            } else {
//                UIHelper.showValidationError(
//						"There are errors in this view. Please fix them first or cancel.");
//            }
//        }
//    }

//	private void addFooterHooks() {
//        footer.addSaveHook(this);
//        footer.addCancelHook(this);
//        footer.addEditHook(this);
//    }

//    @Override
//    public void setEnabled(boolean enabled) {
//        super.setEnabled(enabled);
//        footer.setVisible(enabled);
//    }

//    @Override
//    public void viewStateChanged(ViewStateChangedEvent event) {
////        switch (event.getNewState()) {
////            case Init:
////            case NonLeafSelected:
////            case Edit:
////                footer.setVisible(event.getSource() == attributesTable);
////                break;
////            case LeafSelected:
////                footer.setVisible(true);
////                break;
////        }
////        attributesTable.viewStateChanged(event);//forward
//    }

//    public void modelChanged(Mbean newModel) {
//        attributesTable.modelChanged(newModel); //forward
//    }
}

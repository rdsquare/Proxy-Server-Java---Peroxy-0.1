/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package peroxy;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumnModel;
import javax.swing.AbstractCellEditor;
import java.awt.Component;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.JDialog;
/**
 *
 * @author rd_square
 */
public class ShowTable extends javax.swing.JDialog implements Runnable{

    /**
     * Store data of active clients
     */
    String[][] data;
    
    /**
     * Store column names for table
     */
    String[] columns;
    
    /**
     * Creates new form active clients
     * @param data - data of active clients
     * @param columns - column name to be assigned to table
     */
    public ShowTable(java.awt.Frame frame, boolean model,String[][] data, String[] columns) {
        super(frame,model);
        initComponents();
        
        //Setting variables
        this.data = data;
        this.columns = columns;
        
        //setting table model to table
        MyTableModel tableModel = new MyTableModel();
        clientTable.setModel(tableModel);
        
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        titleLabel.setText("Total Users ("+ tableModel.getRowCount() +") - " + PeroxyInterface.APPNAME);
        ButtonColumn buttonColumn = new ButtonColumn(clientTable, columns.length-1);
    }
    
    @Override
    public void run() {
        this.setSize(1000, 400);
        this.setPreferredSize(new Dimension(1000,400));
        setVisible(true);
    }
    
    /**
     * Creating customer table model to show button in table
     */
    public class MyTableModel extends DefaultTableModel{
        
        MyTableModel(){
            super(data,columns);
        }
        
        @Override
        public boolean isCellEditable(int row, int col){
            if(col == columns.length-1){
                return true; //Editable when column of button
            }
            return false;
        }
    }
    
    public class ButtonColumn extends AbstractCellEditor implements TableCellRenderer, TableCellEditor, ActionListener {
        
        /**
         * Creating a table
         */
        javax.swing.JTable table;
        
        /**
         * Creating button for rendering
         */
        JButton renderButton;
        
        /**
         * Creating button for editing
         */
        JButton editButton;
        
        /**
         * Creating text for editing and returning
         */
        String text;
        
        /**
         * Creating instance of ButtonColumn
         * @param table - table
         * @param column  - column to render a button
         */
        public ButtonColumn(javax.swing.JTable table, int column){
            super();
            this.table = table;
            renderButton = new JButton();
            
            //setting editing button
            editButton = new JButton();
            editButton.setFocusPainted(false);
            editButton.addActionListener(this);
            
            TableColumnModel columnModel = table.getColumnModel();
            columnModel.getColumn(column).setCellEditor(this);
            columnModel.getColumn(column).setCellRenderer(this);
        }
        
        @Override
        public Component getTableCellRendererComponent(
        javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus,
                int row, int column) {
            if(hasFocus) {
                if(value.toString() == "Block!"){
                    renderButton.setForeground(table.getForeground());
                    renderButton.setBackground(Color.RED);
                } else {
                    renderButton.setForeground(table.getForeground());
                    renderButton.setBackground(Color.GREEN);
                }
            } else if (isSelected) {
                renderButton.setForeground(table.getSelectionForeground());
                renderButton.setBackground(table.getSelectionBackground());
            } else {
                if(value.toString() == "Block!"){
                    renderButton.setForeground(table.getForeground());
                    renderButton.setBackground(Color.RED);
                } else {
                    renderButton.setForeground(table.getForeground());
                    renderButton.setBackground(Color.GREEN);
                }
            }
            renderButton.setText((value==null) ? "" : value.toString());
            return renderButton;
        }
        
        @Override
        public Component getTableCellEditorComponent(
        javax.swing.JTable table,Object value ,boolean isSelected, int row, int column){
            text = (value==null) ? "" : value.toString();
            editButton.setText(text);
            return editButton;
        }
        
        @Override
        public Object getCellEditorValue(){
            return text;
        }
        
        @Override
        public void actionPerformed(ActionEvent e){
            fireEditingStopped();
            if(e.getActionCommand() == "Block!"){ //block client
                PeroxyInterface.SIGNALMESSAGE += "Blocking client with ip " +data[table.getSelectedRow()][1] + "....<br>"; 
                PeroxyInterface.blockClient(data[table.getSelectedRow()][1], data[table.getSelectedRow()][2]);
                table.getModel().setValueAt("Unblock!",table.getSelectedRow() , table.getSelectedColumn());
                PeroxyInterface.SIGNALMESSAGE += "Client blocked by you.<br>";
            } else if(e.getActionCommand() == "Unblock!"){ //Unblock client
                PeroxyInterface.SIGNALMESSAGE += "Unblocking client with ip " +data[table.getSelectedRow()][1] + "....<br>"; 
                PeroxyInterface.unblockClient(data[table.getSelectedRow()][1]);
                table.getModel().setValueAt("Block!",table.getSelectedRow() , table.getSelectedColumn());
                PeroxyInterface.SIGNALMESSAGE += "Client Unblocked by you.<br>";
            } else { //Unblock website
                PeroxyInterface.SIGNALMESSAGE += "Unblocking website " +data[table.getSelectedRow()][2] + "....<br>"; 
                PeroxyInterface.unblockWebsite(data[table.getSelectedRow()][1]);
                ((DefaultTableModel)table.getModel()).removeRow(table.getSelectedRow());
                PeroxyInterface.SIGNALMESSAGE += "Website Unblocked by you.<br>";
            }
        }
    }
    
    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        titleLabel = new javax.swing.JLabel();
        jScrollPane1 = new javax.swing.JScrollPane();
        clientTable = new javax.swing.JTable();
        okButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMaximumSize(new java.awt.Dimension(222222222, 392));
        setMinimumSize(new java.awt.Dimension(640, 392));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowOpened(java.awt.event.WindowEvent evt) {
                formWindowOpened(evt);
            }
        });

        titleLabel.setFont(new java.awt.Font("Ubuntu", 1, 15)); // NOI18N
        titleLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        titleLabel.setText("Active Clients (0) - Peroxy/0.1");

        clientTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {
                "SNO", "Title 2", "Title 3", "Title 4"
            }
        ) {
            boolean[] canEdit = new boolean [] {
                false, false, false, false
            };

            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return canEdit [columnIndex];
            }
        });
        clientTable.setMaximumSize(new java.awt.Dimension(2147483647, 234444423));
        clientTable.setMinimumSize(new java.awt.Dimension(60, 300));
        clientTable.setRequestFocusEnabled(false);
        clientTable.setRowHeight(25);
        clientTable.setRowHeight(25);
        clientTable.setRowSelectionAllowed(false);
        jScrollPane1.setViewportView(clientTable);

        okButton.setFont(new java.awt.Font("Ubuntu", 1, 18)); // NOI18N
        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(titleLabel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 616, Short.MAX_VALUE)
                .addContainerGap())
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 171, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(286, 286, 286))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addComponent(titleLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 41, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 304, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 35, javax.swing.GroupLayout.PREFERRED_SIZE))
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        //clsoging the dialog box
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void formWindowOpened(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowOpened
        // open at the center of window
        this.setLocationRelativeTo(null);
    }//GEN-LAST:event_formWindowOpened

    

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JTable clientTable;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JButton okButton;
    private javax.swing.JLabel titleLabel;
    // End of variables declaration//GEN-END:variables
}

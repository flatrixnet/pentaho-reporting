/*
 * This program is free software; you can redistribute it and/or modify it under the
 * terms of the GNU Lesser General Public License, version 2.1 as published by the Free Software
 * Foundation.
 *
 * You should have received a copy of the GNU Lesser General Public License along with this
 * program; if not, you can obtain a copy at http://www.gnu.org/licenses/old-licenses/lgpl-2.1.html
 * or from the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * Copyright (c) 2009 Pentaho Corporation.  All rights reserved.
 */

package org.pentaho.reporting.designer.core.editor.report.elements;

import java.awt.Window;
import java.util.Locale;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import org.pentaho.reporting.designer.core.Messages;
import org.pentaho.reporting.designer.core.ReportDesignerContext;
import org.pentaho.reporting.designer.core.editor.ReportRenderContext;
import org.pentaho.reporting.designer.core.editor.parameters.SubReportDataSourceDialog;
import org.pentaho.reporting.designer.core.editor.report.ReportElementEditorContext;
import org.pentaho.reporting.designer.core.util.exceptions.UncaughtExceptionsModel;
import org.pentaho.reporting.designer.core.util.undo.BandedSubreportEditUndoEntry;
import org.pentaho.reporting.designer.core.util.undo.ElementEditUndoEntry;
import org.pentaho.reporting.designer.core.util.undo.UndoManager;
import org.pentaho.reporting.engine.classic.core.AbstractReportDefinition;
import org.pentaho.reporting.engine.classic.core.AbstractRootLevelBand;
import org.pentaho.reporting.engine.classic.core.Band;
import org.pentaho.reporting.engine.classic.core.ReportDataFactoryException;
import org.pentaho.reporting.engine.classic.core.SubReport;
import org.pentaho.reporting.engine.classic.core.metadata.ElementMetaData;
import org.pentaho.reporting.engine.classic.core.metadata.ElementType;
import org.pentaho.reporting.engine.classic.extensions.parsers.reportdesigner.ReportDesignerParserModule;
import org.pentaho.reporting.libraries.designtime.swing.LibSwingUtil;

/**
 * Subreport drag handler
 *
 * @author Thomas Morgner
 */
public class SubreportReportElementDragHandler extends BaseReportElementDragHandler
{

  public SubreportReportElementDragHandler()
  {
    super();
  }

  protected void invokeConfigureHandler(final SubReport visualElement,
                                        final Band band,
                                        final ReportElementEditorContext dragContext,
                                        final boolean rootBand)
  {
    SwingUtilities.invokeLater(new SubreportConfigureHandler(visualElement, band, dragContext, rootBand));
  }

  /**
   * Subreport specific handling.  Create the visual element and set appropriate attributes
   * @param elementMetaData
   * @return
   * @throws Exception
   */
  protected SubReport setReportStyle(final ElementMetaData elementMetaData) throws Exception
  {
    try
    {
      // Create a subreport element
      final ElementType type = elementMetaData.create();
      final SubReport visualElement = new SubReport();
      visualElement.getRelationalGroup(0).getHeader().setAttribute(ReportDesignerParserModule.NAMESPACE, ReportDesignerParserModule.HIDE_IN_LAYOUT_GUI_ATTRIBUTE,Boolean.TRUE);
      visualElement.getRelationalGroup(0).getFooter().setAttribute(ReportDesignerParserModule.NAMESPACE, ReportDesignerParserModule.HIDE_IN_LAYOUT_GUI_ATTRIBUTE,Boolean.TRUE);
      visualElement.getDetailsFooter().setAttribute(ReportDesignerParserModule.NAMESPACE, ReportDesignerParserModule.HIDE_IN_LAYOUT_GUI_ATTRIBUTE,Boolean.TRUE);
      visualElement.getDetailsHeader().setAttribute(ReportDesignerParserModule.NAMESPACE, ReportDesignerParserModule.HIDE_IN_LAYOUT_GUI_ATTRIBUTE, Boolean.TRUE);
      visualElement.getNoDataBand().setAttribute(ReportDesignerParserModule.NAMESPACE, ReportDesignerParserModule.HIDE_IN_LAYOUT_GUI_ATTRIBUTE, Boolean.TRUE);
      visualElement.getWatermark().setAttribute(ReportDesignerParserModule.NAMESPACE, ReportDesignerParserModule.HIDE_IN_LAYOUT_GUI_ATTRIBUTE,Boolean.TRUE);

      type.configureDesignTimeDefaults(visualElement, Locale.getDefault());

      return visualElement;
    }
    catch (Exception e)
    {
      UncaughtExceptionsModel.getInstance().addException(e);
      throw e;
    }
  }

  private static class SubreportConfigureHandler implements Runnable
  {
    private SubReport subReport;
    private Band parent;
    private ReportElementEditorContext dragContext;
    private boolean rootband;

    private SubreportConfigureHandler(final SubReport subReport,
                                      final Band parent,
                                      final ReportElementEditorContext dragContext,
                                      final boolean rootband)
    {
      this.subReport = subReport;
      this.parent = parent;
      this.dragContext = dragContext;
      this.rootband = rootband;
    }

    public void run()
    {
      if (rootband)
      {
        final int result = JOptionPane.showOptionDialog(dragContext.getRepresentationContainer(),
            Messages.getString("SubreportReportElementDragHandler.BandedOrInlineSubreportQuestion"),
            Messages.getString("SubreportReportElementDragHandler.InsertSubreport"),
            JOptionPane.YES_NO_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null,
            new String[]{Messages.getString("SubreportReportElementDragHandler.Inline"),
                Messages.getString("SubreportReportElementDragHandler.Banded"),
                Messages.getString("SubreportReportElementDragHandler.Cancel")},
            Messages.getString("SubreportReportElementDragHandler.Inline"));
        if (result == JOptionPane.CLOSED_OPTION || result == 2)
        {
          return;
        }

        if (result == 0)
        {
          final ReportRenderContext context = dragContext.getRenderContext();
          final UndoManager undo = context.getUndo();
          undo.addChange(Messages.getString("SubreportReportElementDragHandler.UndoEntry"),
              new ElementEditUndoEntry(parent.getObjectID(), parent.getElementCount(), null, subReport));
          parent.addElement(subReport);
        }
        else
        {
          final AbstractRootLevelBand arb = (AbstractRootLevelBand) parent;

          final ReportRenderContext context = dragContext.getRenderContext();
          final UndoManager undo = context.getUndo();
          undo.addChange(Messages.getString("SubreportReportElementDragHandler.UndoEntry"),
              new BandedSubreportEditUndoEntry(parent.getObjectID(), arb.getSubReportCount(), null, subReport));
          arb.addSubReport(subReport);
        }
      }
      else
      {
        final ReportRenderContext context = dragContext.getRenderContext();
        final UndoManager undo = context.getUndo();
        undo.addChange(Messages.getString("SubreportReportElementDragHandler.UndoEntry"),
            new ElementEditUndoEntry(parent.getObjectID(), parent.getElementCount(), null, subReport));
        parent.addElement(subReport);
      }

      final ReportDesignerContext designerContext = dragContext.getDesignerContext();
      final Window window = LibSwingUtil.getWindowAncestor(designerContext.getParent());
      final AbstractReportDefinition reportDefinition = designerContext.getActiveContext().getReportDefinition();

      try
      {
        // Create the new subreport tab and update the active context to point to new subreport.
        subReport.setDataFactory(reportDefinition.getDataFactory());

        final int idx = designerContext.addSubReport(designerContext.getActiveContext(), subReport);
        designerContext.setActiveContext(designerContext.getReportRenderContext(idx));
      }
      catch (ReportDataFactoryException e)
      {
        UncaughtExceptionsModel.getInstance().addException(e);
      }

      // Prompt user to either create or use an existing data-source.
      final SubReportDataSourceDialog subreportDataSourceDialog;
      subreportDataSourceDialog = new SubReportDataSourceDialog((JFrame)window);

      final String queryName = subreportDataSourceDialog.performSelection(designerContext);
      if (queryName != null)
      {
        subReport.setQuery(queryName);
      }

      dragContext.getRenderContext().getSelectionModel().setSelectedElements(new Object[]{subReport});
    }
  }
}
/**
 * React and React Hook Imports:
 * - useState: Manages local component state (like selected elements, load states, input text).
 * - useCallback: Caches function definitions across renders to prevent unnecessary re-rendering of child elements.
 * - useMemo: Memoizes expensive computations (like finding the active node from the state list) so they only run when dependencies change.
 */
import { useState, useCallback, useMemo } from 'react';

/**
 * React Flow Diagram Engine Imports:
 * - ReactFlow: The interactive diagram canvas container.
 * - MiniMap: Pin-point overview map of the canvas.
 * - Controls: Floating action controls for zooming/panning/fitting the canvas.
 * - Background: Customizable background grid pattern.
 * - useNodesState: React Flow hook to easily manage state for node boxes.
 * - useEdgesState: React Flow hook to easily manage state for connection lines.
 * - addEdge: Utility to link two node boxes together with a new connection edge.
 * - MarkerType: Enum representing arrow-heads/markers at the end of connection edges.
 * - Connection, Edge, Node: TypeScript interface definitions for React Flow's engine structure.
 */
import {
  ReactFlow,
  MiniMap,
  Controls,
  Background,
  useNodesState,
  useEdgesState,
  addEdge,
  MarkerType,
  type Connection,
  type Edge,
  type Node,
} from '@xyflow/react';

/**
 * Lucide React Icons:
 * - Beautiful, clean SVG icons to visually represent UI controls (Plus, Trash, Settings, etc.).
 */
import { Plus, Trash2, Layers, ArrowLeftRight, Settings, Type, LayoutGrid, FileText } from 'lucide-react';

/**
 * Router Link:
 * - Enables SPA navigation without full page reloads to go back to the Home Hub page.
 */
import { Link } from 'react-router-dom';

/**
 * Custom Component Imports:
 * - ThemeToggle: Handles switching between light and dark UI themes.
 */
import ThemeToggle from './ThemeToggle';

/**
 * CSS Styling:
 * - Standard style definitions required by React Flow for layout positioning and canvas grid systems.
 */
import '@xyflow/react/dist/style.css';

/**
 * Types & Domain Model:
 * - Field: Representation of individual attributes (name, type) belonging to an entity.
 * - EntityNode: Describes the structure and UML representation of each custom entity class block.
 * - RelationshipType: Union type defining relationship connections (ManyToMany, OneToMany, OneToOne, Inheritance).
 */
import { type Field, type EntityNode, type RelationshipType } from '../types/modeling';

/**
 * Predefined Design Pattern Blueprints:
 * - DESIGN_PATTERN_TEMPLATES: Stores structural templates (Singleton, Factory, Observer, Decorator, Strategy) to instantly bootstrap the canvas.
 */
import { DESIGN_PATTERN_TEMPLATES } from '../constants/patterns';

/**
 * Custom Canvas Node Component:
 * - EntityNodeComponent: A custom React Flow component rendering UML-styled blocks with database tables look.
 */
import { EntityNodeComponent } from './EntityNodeComponent';

/**
 * Backend Generator API Client:
 * - generateProject: Connects to the Spring Boot microservices backend to convert JDL script to a complete generated ZIP project.
 */
import { generateProject } from '../api/generatorApi';

/**
 * Custom Node Types Mapper:
 * - Map the string key 'entityNode' to our Custom React Node component. React Flow uses this to render customized designs inside the canvas.
 */
const nodeTypes = { entityNode: EntityNodeComponent };

/**
 * Helper: Style Connection Edges:
 * - Maps relationship types (like OneToMany, ManyToMany, Inheritance) to visual stroke, labels, and marker arrow shapes.
 * - Inheritance uses dashed lines ('5,5') and red highlights, while standard associations use solid indigo lines.
 */
const getEdgeStyle = (typeLabel: string) => {
  const isInheritance = typeLabel === 'Inheritance' || typeLabel === 'extends';
  return {
    // Shows 'extends' text on the link line for inheritance, otherwise shows association type label.
    label: isInheritance ? 'extends' : typeLabel,
    // Styles: Dashed red borders for inheritance class hierarchy, solid indigo for standard database references.
    style: isInheritance ? { strokeDasharray: '5,5', strokeWidth: 2, stroke: '#ef4444' } : { strokeWidth: 2, stroke: '#6366f1' },
    // Arrow marker styles indicating the target path direction.
    markerEnd: { type: MarkerType.ArrowClosed, color: isInheritance ? '#ef4444' : '#6366f1' }
  };
};

/**
 * MAIN COMPONENT: ModellingEditor
 * - Represents the entire interactive workspace comprising the flow-canvas, the properties inspector, and the JDL code editor.
 */
export default function ModellingEditor() {
  /**
   * State: Active Entity Nodes:
   * - React Flow state manager tracking coordinates, labels, fields, and operations for each entity box.
   */
  const [nodes, setNodes, onNodesChange] = useNodesState<EntityNode>([]);

  /**
   * State: Connection Edges:
   * - Tracks directional relationship lines between different diagram blocks on the canvas.
   */
  const [edges, setEdges, onEdgesChange] = useEdgesState<Edge>([]);

  /**
   * State: Selected Link Relationship:
   * - Keeps track of which line style is active in the toolbar dropdown (e.g. ManyToMany, OneToOne, etc.) so new lines drawn adopt this type.
   */
  const [relationshipType, setRelationshipType] = useState<RelationshipType>('ManyToMany');

  /**
   * State: Active Element Selection:
   * - Tracks whether the user clicked a node block or relationship edge line, letting us open the sidebar properties inspector.
   */
  const [activeSelection, setActiveSelection] = useState<{ type: 'node' | 'edge'; id: string } | null>(null);

  /**
   * State: Raw JDL Script Buffer:
   * - Holds the JDL (JHipster Domain Language) schema syntax inside the code editor area.
   */
  const [inputJDL, setInputJDL] = useState<string>(
    `entity Student {\n  email String\n}\n\nentity Course {\n  title String\n}\n\nrelationship ManyToMany {\n  Student to Course\n}`
  );

  /**
   * State: Code Generation Loading Indicator:
   * - Disables buttons and shows a spinner icon when a project ZIP compile request is currently pending with the backend.
   */
  const [isGenerating, setIsGenerating] = useState(false);

  /**
   * State: Workspace Notifications:
   * - Triggers error alert banners or success alerts on the sidebar (e.g. empty canvas warn or compilation status).
   */
  const [notification, setNotification] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  /**
   * Callback: Load Pre-configured Design Pattern Templates:
   * - Instantly populates the nodes and edges by deep copying (structuredClone) preset design patterns like Singleton or Observer.
   */
  const handleLoadPatternTemplate = (patternName: string) => {
    if (!patternName) return;
    const blueprint = DESIGN_PATTERN_TEMPLATES[patternName];
    if (blueprint) {
      // Clones nodes array deep to prevent visual positioning modifications from corrupting reference templates.
      setNodes(structuredClone(blueprint.nodes));
      // Clones connections array deep.
      setEdges(structuredClone(blueprint.edges));
      // Deselects current item to clear property panel focus.
      setActiveSelection(null);
      
      // Updates script textbox with instructional comment explaining template loaded.
      const structuralInfo = `// Interactive Template: Loaded ${patternName} Pattern.\n// Modify fields/methods or extend elements on the canvas workspace, then click Generate.`;
      setInputJDL(structuralInfo);
    }
  };

  /**
   * Callback: Selection Change listener:
   * - Fires whenever the user clicks an item on the canvas. Toggles open the customized sidebar properties matching the selection.
   */
  const onSelectionChange = useCallback(({ nodes: selectedNodes, edges: selectedEdges }: { nodes: Node[]; edges: Edge[] }) => {
    if (selectedNodes.length > 0) {
      // Prioritize inspecting the clicked entity box first.
      setActiveSelection({ type: 'node', id: selectedNodes[0].id });
    } else if (selectedEdges.length > 0) {
      // Inspect the clicked line.
      setActiveSelection({ type: 'edge', id: selectedEdges[0].id });
    } else {
      // Clicked on empty canvas, clear sidebar inspector view back to raw text code editor.
      setActiveSelection(null);
    }
  }, []);

  /**
   * Callback: Update Specific Node's Fields/Data:
   * - Iterates over nodes and applies a partial update to the targeted entity (modifying labels, fields, or methods).
   */
  const updateNodeData = useCallback((id: string, updater: (data: EntityNode['data']) => Partial<EntityNode['data']>) => {
    setNodes((nds) => nds.map((n) => (n.id === id ? { ...n, data: { ...n.data, ...updater(n.data) } } : n)));
  }, [setNodes]);

  /**
   * Callback: Update Specific Edge's Label/Style:
   * - Modifies relationship rules of a connecting line and recalibrates visual styles (solid/dashed arrows).
   */
  const updateEdgeType = useCallback((edgeId: string, typeLabel: string) => {
    setEdges((eds) => eds.map((e) => {
      if (e.id !== edgeId) return e;
      return {
        ...e,
        ...getEdgeStyle(typeLabel)
      };
    }));
  }, [setEdges]);

  /**
   * Callback: Delete Node Block:
   * - Safely filters out the targeted node block from array state.
   * - Automatically clears out orphan connection lines starting from or pointing to the deleted block.
   */
  const deleteSelectedEntity = useCallback((id: string) => {
    setNodes((nds) => nds.filter((n) => n.id !== id));
    setEdges((eds) => eds.filter((e) => e.source !== id && e.target !== id));
    setActiveSelection(null);
  }, [setNodes, setEdges]);

  /**
   * Callback: Delete Edge Line:
   * - Removes a relationship link by filtering out its UUID from the list.
   */
  const deleteSelectedEdge = useCallback((id: string) => {
    setEdges((eds) => eds.filter((e) => e.id !== id));
    setActiveSelection(null);
  }, [setEdges]);

  /**
   * Callback: Add New Entity Box:
   * - Instantiates a new UML diagram box at a slightly randomized position to avoid stacked blocks.
   * - Automatically selects the newly created block so you can start adding attributes immediately.
   */
  const addNewEntity = useCallback(() => {
    const id = crypto.randomUUID();
    const newNode: EntityNode = {
      id,
      type: 'entityNode',
      // Random coordinates offset near center so it spawns dynamically on top.
      position: { x: 150 + Math.random() * 100, y: 150 + Math.random() * 100 },
      data: { label: `NewEntity${nodes.length + 1}`, fields: [], methods: [] },
    };
    setNodes((nds) => nds.concat(newNode));
    setActiveSelection({ type: 'node', id });
  }, [nodes.length, setNodes]);

  /**
   * Callback: Clear Canvas Workspace:
   * - Requests safety confirmation, then wipes both nodes and relationship lists completely.
   */
  const clearAllNodesAndEdges = useCallback(() => {
    if (window.confirm("Are you sure you want to completely erase the canvas layout?")) {
      setNodes([]);
      setEdges([]);
      setActiveSelection(null);
      setInputJDL('');
    }
  }, [setNodes, setEdges]);

  /**
   * Callback: Load Academic Sample Script:
   * - Pre-populates the text editor with a multi-entity database schema (Professors, Departments, Projects) to demo complex structures.
   */
  const loadComplexJDLScriptSample = useCallback(() => {
    setInputJDL(`entity Professor {\n  name String\n  email String\n}\n\nentity Department {\n  title String\n  budget BigDecimal\n}\n\nentity GraduateProject {\n  topic String\n  deadline LocalDate\n}\n\nrelationship Inheritance {\n  Department to Professor\n}\n\nrelationship OneToOne {\n  Professor to GraduateProject\n}`);
  }, []);

  /**
   * Callback: Generate JDL Script & Compile Backend Project Zip:
   * - Step 1: Generates a plain-text JDL schema script by translating diagram nodes and edges.
   * - Step 2: Handles extending parent relationships ('extends' UML syntax translation).
   * - Step 3: Translates associations (OneToOne, OneToMany, ManyToMany) into valid relationship structures.
   * - Step 4: Calls backend microservices API to generate full source code scaffolding and starts a browser ZIP file download.
   */
  const generateJDLFromCanvas = useCallback(async () => {
    let jdlString = '';
    const inheritanceMap: Record<string, string> = {};

    // First scan all relationship lines to extract active inheritance links.
    edges.forEach((e) => {
      if (e.label === 'extends') {
        const parentNode = nodes.find((n) => n.id === e.target);
        if (parentNode) inheritanceMap[e.source] = parentNode.data.label;
      }
    });

    // Translate each visual node box into standard JDL 'entity' syntax blocks.
    nodes.forEach((n) => {
      const parentName = inheritanceMap[n.id];
      const extendsClause = parentName ? ` extends ${parentName}` : '';
      jdlString += `entity ${n.data.label}${extendsClause} {\n`;
      n.data.fields.forEach((f) => { jdlString += `  ${f.name} ${f.type}\n`; });
      jdlString += `}\n\n`;
    });

    // Map visual relationships (OneToOne, OneToMany, ManyToMany) to corresponding JDL connection code blocks.
    ['OneToOne', 'OneToMany', 'ManyToMany'].forEach((type) => {
      const edgeList = edges.filter((e) => e.label === type);
      if (edgeList.length === 0) return;
      jdlString += `relationship ${type} {\n`;
      edgeList.forEach((e) => {
        const s = nodes.find((n) => n.id === e.source);
        const t = nodes.find((n) => n.id === e.target);
        if (s && t) {
          // Many-to-Many connections require bidirection properties inside bracket structures.
          jdlString += type === 'ManyToMany'
            ? `  ${s.data.label}{${t.data.label.toLowerCase()}s} to ${t.data.label}{${s.data.label.toLowerCase()}s}\n`
            : `  ${s.data.label} to ${t.data.label}\n`;
        }
      });
      jdlString += `}\n\n`;
    });

    const finalCDL = jdlString.trim();
    // Update raw JDL script text area with generated output.
    setInputJDL(finalCDL || '// No visual structure configured.');
    // Close active inspector selection to display raw script editor section.
    setActiveSelection(null);

    // Safeguard: Halt compilation process if canvas workspace is entirely blank.
    if (!finalCDL) {
      setNotification({
        type: 'error',
        message: 'Project generation failed. No entities or structures are configured on the canvas.'
      });
      return;
    }

    setIsGenerating(true);
    setNotification(null);

    try {
      // Send JDL schema text script payload to backend spring-boot builder.
      const blob = await generateProject(finalCDL);
      
      // Programmatically create hidden anchor element to initiate instant file download in browser.
      const downloadUrl = window.URL.createObjectURL(blob);
      const downloadLink = document.createElement('a');
      downloadLink.href = downloadUrl;
      downloadLink.download = 'generated-project.zip';
      document.body.appendChild(downloadLink);
      downloadLink.click();
      document.body.removeChild(downloadLink);
      window.URL.revokeObjectURL(downloadUrl);

      setNotification({
        type: 'success',
        message: 'Project generated successfully.'
      });
    } catch (error) {
      console.error(error);
      const errorMessage = error instanceof Error ? error.message : 'Project generation failed. Please check your model and try again.';
      setNotification({
        type: 'error',
        message: errorMessage
      });
    } finally {
      setIsGenerating(false);
    }
  }, [nodes, edges]);

  /**
   * Callback: Parse Raw JDL script text back to visual Diagram:
   * - Uses regular expression match patterns to extract entity definitions, extending superclasses, attribute fields, and relationships.
   * - Dynamically calculates layout positions to distribute parsed boxes nicely across the screen.
   */
  const parseJDLToCanvas = useCallback(() => {
    try {
      const parsedNodes: EntityNode[] = [];
      const parsedEdges: Edge[] = [];

      // Regex matching standard 'entity EntityName [extends SuperName] { ... }' formats.
      const entityBlocks = inputJDL.match(/entity\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{([^}]*)\}/g);
      
      if (entityBlocks) {
        entityBlocks.forEach((block, index) => {
          const match = block.match(/entity\s+(\w+)(?:\s+extends\s+(\w+))?\s*\{([^}]*)\}/);
          if (!match) return;

          const [, entityName, parentName, contentBody] = match;
          const fields: Field[] = [];

          // Parse field attributes by reading lines inside curly brackets.
          contentBody.split('\n').map(l => l.trim()).filter(Boolean).forEach(line => {
            const parts = line.split(/\s+/);
            if (parts.length >= 2) {
              fields.push({ id: crypto.randomUUID(), name: parts[0], type: parts[1] });
            } else if (parts.length === 1 && parts[0]) {
              // Defaults field type to String if type definition is missing.
              fields.push({ id: crypto.randomUUID(), name: parts[0], type: 'String' });
            }
          });

          const entityId = entityName.toLowerCase() + '-id';
          // Compute positions dynamically (columns of 3) to distribute parsed cards.
          parsedNodes.push({
            id: entityId,
            type: 'entityNode',
            position: { x: 100 + (index % 3) * 280, y: 150 + Math.floor(index / 3) * 320 },
            data: { label: entityName, fields, methods: [] }
          });

          // If standard 'extends' inheritance is matched, generate a red dashed inheritance link.
          if (parentName) {
            parsedEdges.push({
              id: `edge-inherit-${crypto.randomUUID()}`,
              source: entityId,
              target: parentName.toLowerCase() + '-id',
              type: 'default',
              ...getEdgeStyle('extends')
            });
          }
        });
      }

      // Regex matching relationship blocks (e.g., 'relationship OneToMany { Source to Target }').
      const relationshipBlocks = inputJDL.match(/relationship\s+(\w+)\s*\{([^}]*)\}/g);
      if (relationshipBlocks) {
        relationshipBlocks.forEach(block => {
          const match = block.match(/relationship\s+(\w+)\s*\{([^}]*)\}/);
          if (!match) return;

          const [, relType, contentBody] = match;
          contentBody.split('\n').map(l => l.trim()).filter(Boolean).forEach(line => {
            // Extracts matching source and target entities (ignoring extra JDL bidirection details).
            const linkMatch = line.match(/(\w+)(?:\{.*\})?\s+to\s+(\w+)/);
            if (linkMatch) {
              const [, sourceEnt, targetEnt] = linkMatch;
              parsedEdges.push({
                id: `edge-rel-${crypto.randomUUID()}`,
                source: sourceEnt.toLowerCase() + '-id',
                target: targetEnt.toLowerCase() + '-id',
                type: 'default',
                ...getEdgeStyle(relType)
              });
            }
          });
        });
      }

      // Warn if the parser fails to identify any structure.
      if (parsedNodes.length === 0) {
        alert("Parser complete: No valid structures identified.");
        return;
      }

      // Update state with newly reconstructed diagram data.
      setNodes(parsedNodes);
      setEdges(parsedEdges);
      setActiveSelection(null);
    } catch {
      alert("Parsing Error: Verify your JDL configuration layout syntax.");
    }
  }, [inputJDL, setNodes, setEdges]);

  /**
   * Callback: Handle Link Creation by Dragging:
   * - Triggered automatically by React Flow when dragging a connector line from source handle to target handle.
   * - Configures the styling of the new connection based on the currently selected relationship type.
   */
  const onConnect = useCallback((params: Connection) => {
    const stylizedEdge: Edge = {
      id: `edge-${crypto.randomUUID()}`,
      source: params.source,
      target: params.target,
      sourceHandle: params.sourceHandle,
      targetHandle: params.targetHandle,
      type: 'default',
      ...getEdgeStyle(relationshipType)
    };
    setEdges((eds) => addEdge(stylizedEdge, eds));
  }, [relationshipType, setEdges]);

  /**
   * Callback: Flip Line Cardinality Direction:
   * - Swaps the target and source of a selected relationship line, reversing the arrow flow.
   */
  const toggleEdgeDirection = useCallback((edgeId: string) => {
    setEdges((eds) => eds.map((e) => (e.id === edgeId ? { ...e, source: e.target, target: e.source } : e)));
  }, [setEdges]);

  /**
   * Callback: Export JDL Schema Script File:
   * - Bundles inputJDL text buffer into a text/plain Blob and initiates a local browser download for 'model.jdl'.
   */
  const downloadJDLHandler = useCallback(() => {
    const element = document.createElement('a');
    element.href = URL.createObjectURL(new Blob([inputJDL], { type: 'text/plain' }));
    element.download = 'model.jdl';
    document.body.appendChild(element);
    element.click();
    document.body.removeChild(element);
  }, [inputJDL]);

  /**
   * Memoized UI Data Selectors:
   * - selectedNode: Returns the Node model if activeSelection is type 'node'.
   * - selectedEdge: Returns the Edge model if activeSelection is type 'edge'.
   * - currentParentNode: Detects if the selected entity extends another node so we can pre-select parent dropdown selection.
   */
  const selectedNode = useMemo(() => activeSelection?.type === 'node' ? nodes.find(n => n.id === activeSelection.id) : null, [activeSelection, nodes]);
  const selectedEdge = useMemo(() => activeSelection?.type === 'edge' ? edges.find(e => e.id === activeSelection.id) : null, [activeSelection, edges]);
  const currentParentNode = useMemo(() => {
    if (!selectedNode) return null;
    const parentEdge = edges.find(e => e.source === selectedNode.id && e.label === 'extends');
    return parentEdge ? nodes.find(n => n.id === parentEdge.target) : null;
  }, [selectedNode, edges, nodes]);

  /**
   * Callback: Handle Changing Parent Class:
   * - Clears any existing 'extends' relationship edge for this node.
   * - Instantiates a new inheritance relationship line pointing to the newly selected target superclass.
   */
  const handleParentChange = useCallback((nodeId: string, targetParentId: string) => {
    setEdges(eds => eds.filter(e => !(e.source === nodeId && e.label === 'extends')));
    if (targetParentId === 'none') return;

    const parentEdgeSpec: Edge = {
      id: `edge-inherit-${crypto.randomUUID()}`,
      source: nodeId,
      target: targetParentId,
      type: 'default',
      ...getEdgeStyle('extends')
    };
    setEdges(eds => eds.concat(parentEdgeSpec));
  }, [setEdges]);

  return (
    <div className="editor-window">
      {/* Visual Canvas Pane */}
      <div className="canvas-pane">
        {/* Top Navbar Workspace Controls */}
        <div className="navbar-controls">
          <div className="nav-brand-area">
            <Link to="/" className="btn-back">← Home Hub</Link>
            <div className="nav-titles">
              <span className="editor-title">CodeClassroom</span>
              <span className="editor-tag">JDL Engine v2.0</span>
            </div>
          </div>

          <div className="toolbar-actions">
            {/* Quick Templates Pattern Selector */}
            {/* <div className="selector-box custom-pattern-dropdown-wrapper">
              <LayoutGrid size={13} className="pattern-decorator-icon" />
              <select
                defaultValue=""
                onChange={(e) => {
                  handleLoadPatternTemplate(e.target.value);
                  e.target.value = "";
                }}
                className="select-dropdown pattern-select-menu"
              >
                <option value="" disabled>Examples / Patterns</option>
                <option value="Singleton">Singleton</option>
                <option value="Factory">Factory</option>
                <option value="Observer">Observer</option>
                <option value="Decorator">Decorator</option>
                <option value="Strategy">Strategy</option>
              </select>
            </div> */}

            {/* Link Edge Relationship Selector */}
            <div className="selector-box">
              <Layers size={13} style={{ color: '#475569' }} /> 
              <span>Add Relation:</span>
              <select value={relationshipType} onChange={(e) => setRelationshipType(e.target.value as RelationshipType)} className="select-dropdown">
                <option value="ManyToMany">ManyToMany</option>
                <option value="OneToMany">OneToMany</option>
                <option value="OneToOne">OneToOne</option>
                <option value="Inheritance">Inheritance</option>
              </select>
            </div>

            {/* Quick Add Node and Clear Canvas Buttons */}
            <button onClick={addNewEntity} className="btn-action-green">
              <Plus size={15} /> Add Entity
            </button>

            <button onClick={clearAllNodesAndEdges} className="btn-action-red">
              <Trash2 size={14} /> Clear Canvas
            </button>
            <ThemeToggle />
          </div>
        </div>

        {/* Main Infinite Flow Diagram Area */}
        <div style={{ flexGrow: 1, width: '100%', height: '100%', position: 'relative', backgroundColor: 'var(--bg-canvas)' }}>
          <ReactFlow 
            nodes={nodes} 
            edges={edges} 
            onNodesChange={onNodesChange} 
            onEdgesChange={onEdgesChange} 
            onConnect={onConnect} 
            nodeTypes={nodeTypes}
            onSelectionChange={onSelectionChange}
            selectNodesOnDrag={false}
            fitView
          >
            <Background color="var(--border-main)" gap={20} size={1} />
            <Controls />
            <MiniMap nodeColor="#1e1b4b" maskColor="var(--shadow-small)" style={{ backgroundColor: 'var(--bg-card)', border: '1px solid var(--border-main)' }} />
          </ReactFlow>
        </div>
      </div>

      {/* Properties Sidebar Panel / JDL Script Editor */}
      <div className="developer-sidebar">
        {/* Dynamic Header: Flips titles based on whether an item is selected on the canvas */}
        <div className="sidebar-section-header">
          {activeSelection ? <Settings size={13} /> : <FileText size={13} />}
          {activeSelection ? `${activeSelection.type.toUpperCase()} PROPERTIES` : 'RAW CDL SCRIPT EDITOR'}
        </div>

        <div className="sidebar-main-scroller">
          {/* Notification Alert Banner for Compile States */}
          {notification && (
            <div 
              className={`notification-banner ${notification.type}`} 
              style={{ 
                padding: '0.75rem', 
                margin: '1rem', 
                borderRadius: '4px', 
                fontSize: '0.85rem', 
                fontWeight: 500, 
                backgroundColor: notification.type === 'success' ? '#065f46' : '#991b1b', 
                color: '#fff', 
                display: 'flex', 
                justifyContent: 'space-between', 
                alignItems: 'center' 
              }}
            >
              <span>{notification.message}</span>
              <button 
                onClick={() => setNotification(null)} 
                style={{ 
                  background: 'none', 
                  border: 'none', 
                  color: '#fff', 
                  cursor: 'pointer', 
                  fontSize: '1rem', 
                  fontWeight: 'bold',
                  marginLeft: '0.5rem'
                }}
              >
                ×
              </button>
            </div>
          )}

          {/* SIDEBAR VIEW 1: ENTITY PROPERTY INSPECTOR */}
          {selectedNode && (
            <div className="inspector-container">
              {/* Entity Class Name Attribute Input */}
              <div className="inspector-group">
                <label className="inspector-field-label">Entity Name</label>
                <div className="inspector-input-wrapper">
                  <Type size={12} className="input-decorator-icon" />
                  <input
                    type="text"
                    value={selectedNode.data.label}
                    onChange={(e) => updateNodeData(selectedNode.id, () => ({ label: e.target.value }))}
                    className="inspector-text-input"
                    placeholder="ClassName"
                  />
                </div>
              </div>

              {/* Inherits Parent Class Dropdown Option */}
              <div className="inspector-group" style={{ marginTop: '0.75rem' }}>
                <label className="inspector-field-label">Extends Parent Class</label>
                <select
                  value={currentParentNode ? currentParentNode.id : 'none'}
                  onChange={(e) => handleParentChange(selectedNode.id, e.target.value)}
                  className="inspector-select-large"
                >
                  <option value="none">-- None (Root Entity) --</option>
                  {nodes
                    .filter((n) => n.id !== selectedNode.id)
                    .map((n) => (
                      <option key={n.id} value={n.id}>{n.data.label}</option>
                    ))}
                </select>
              </div>

              {/* Entity Attribute Fields Sub-Section */}
              <div className="inspector-group border-top-divider" style={{ marginTop: '1.25rem', paddingTop: '1rem' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '0.5rem' }}>
                  <label className="inspector-field-label">Fields / Attributes</label>
                  <button
                    onClick={() => updateNodeData(selectedNode.id, (data) => {
                      const currentFields = data.fields || [];
                      return {
                        fields: [...currentFields, { id: crypto.randomUUID(), name: `field${currentFields.length + 1}`, type: 'String' }]
                      };
                    })}
                    className="btn-inspector-add"
                  >
                    + Field
                  </button>
                </div>
                {selectedNode.data.fields.length === 0 && <p className="uml-empty-text">No fields mapped on this target block.</p>}
                
                {/* Dynamically List Fields */}
                {selectedNode.data.fields.map((f) => (
                  <div key={f.id} className="inspector-row-item">
                    {/* Attribute Name textfield */}
                    <input
                      type="text"
                      value={f.name}
                      placeholder="attribute"
                      onChange={(e) => updateNodeData(selectedNode.id, (data) => ({
                        fields: data.fields.map((field) => (field.id === f.id ? { ...field, name: e.target.value } : field))
                      }))}
                      className="inspector-row-input"
                    />
                    {/* Database Attribute Types Select */}
                    <select
                      value={f.type}
                      onChange={(e) => updateNodeData(selectedNode.id, (data) => ({
                        fields: data.fields.map((field) => (field.id === f.id ? { ...field, type: e.target.value } : field))
                      }))}
                      className="inspector-row-select"
                    >
                      <option value="String">String</option>
                      <option value="Integer">Integer</option>
                      <option value="Long">Long</option>
                      <option value="BigDecimal">BigDecimal</option>
                      <option value="LocalDate">LocalDate</option>
                      <option value="Boolean">Boolean</option>
                    </select>
                    {/* Delete Specific Attribute Row */}
                    <button
                      onClick={() => updateNodeData(selectedNode.id, (data) => ({
                        fields: data.fields.filter((field) => field.id !== f.id)
                      }))}
                      className="btn-row-delete"
                    >
                      <Trash2 size={11} />
                    </button>
                  </div>
                ))}
              </div>
              
              
              {/* Quick Delete Selected Entity Block Button */}
              <div className="inspector-group border-top-divider" style={{ marginTop: '1.5rem', paddingTop: '1rem' }}>
                <button onClick={() => deleteSelectedEntity(selectedNode.id)} className="btn-inspector-delete-entity">
                  <Trash2 size={12} /> Delete Entity Diagram Block
                </button>
              </div>
            </div>
          )}

          {/* SIDEBAR VIEW 2: RELATIONSHIP EDGE INSPECTOR */}
          {selectedEdge && (
            <div className="inspector-container">
              {/* Change Relationship Category Dropdown */}
              <div className="inspector-group">
                <label className="inspector-field-label">Relationship Line Mapping Pattern</label>
                <select
                  value={selectedEdge.label === 'extends' ? 'Inheritance' : String(selectedEdge.label)}
                  onChange={(e) => updateEdgeType(selectedEdge.id, e.target.value)}
                  className="inspector-select-large"
                >
                  <option value="ManyToMany">ManyToMany</option>
                  <option value="OneToMany">OneToMany</option>
                  <option value="OneToOne">OneToOne</option>
                  <option value="Inheritance">Inheritance (Extends Pointer)</option>
                </select>
              </div>

              {/* Flip Direction of Selected Line Connection */}
              <div className="inspector-group" style={{ marginTop: '1rem' }}>
                <button onClick={() => toggleEdgeDirection(selectedEdge.id)} className="btn-sidebar-action btn-slate" style={{ width: '100%', display: 'flex', gap: '0.5rem', justifyContent: 'center', alignItems: 'center' }}>
                  <ArrowLeftRight size={12} /> Flip Cardinality Vector Direction
                </button>
              </div>

              {/* Sever / Delete Association Link */}
              <div className="inspector-group border-top-divider" style={{ marginTop: '1.5rem', paddingTop: '1rem' }}>
                <button onClick={() => deleteSelectedEdge(selectedEdge.id)} className="btn-inspector-delete-entity">
                  <Trash2 size={12} /> Sever Association Path
                </button>
              </div>
            </div>
          )}

          {/* SIDEBAR VIEW 3: RAW JDL CODE SCRIPT WRITER */}
          {!activeSelection && (
            <div style={{ display: 'flex', flexDirection: 'column', height: '100%', width: '100%' }}>
              <div className="sidebar-editor-wrapper" style={{ height: '100%' }}>
                <textarea
                  value={inputJDL}
                  onChange={(e) => setInputJDL(e.target.value)}
                  className="code-editor-textarea"
                  placeholder={`// Write raw CDL schemas here...\nentity Customer {\n  name String\n}`}
                />
              </div>
            </div>
          )}
        </div>

        {/* Floating Sidebar Action Footer Belt */}
        <div className="sidebar-control-belt">
          {/* Action: Load sample schema text */}
          <button onClick={loadComplexJDLScriptSample} className="btn-sidebar-action btn-slate">
            Load Sample
          </button>
          
          {/* Action: Parse raw textbox JDL schema into the visual canvas */}
          <button onClick={parseJDLToCanvas} className="btn-sidebar-action btn-amber">
            Parse CDL
          </button>
          
          {/* Action: Compile visual diagram into a backend spring-boot application zip */}
          <button 
            onClick={generateJDLFromCanvas} 
            className="btn-sidebar-action btn-green"
            disabled={isGenerating}
            style={{ display: 'flex', gap: '0.25rem', justifyContent: 'center', alignItems: 'center' }}
          >
            {isGenerating ? (
              <>
                <svg className="animate-spin" style={{ width: '12px', height: '12px' }} viewBox="0 0 24 24" fill="none">
                  <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                  <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4z"></path>
                </svg>
                Generating...
              </>
            ) : (
              'Generate'
            )}
          </button>
          
          {/* Action: Export raw JDL script as a file download */}
          <button onClick={downloadJDLHandler} className="btn-sidebar-action btn-blue">
            Export
          </button>
        </div>
      </div>
    </div>
  );
}

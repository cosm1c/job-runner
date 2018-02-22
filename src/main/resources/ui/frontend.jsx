"use strict";

// For easier development - not recommended for production
const baseUrl = window.location.protocol === 'file:' ? 'http://localhost:8080' : '';
const wsUrl = window.location.protocol === 'file:' ? `ws://localhost:8080/ws` : `ws://${window.location.host}/ws`;

const WEBSOCKET_CONNECTING = 'WEBSOCKET_CONNECTING';
const WEBSOCKET_CONNECTED = 'WEBSOCKET_CONNECTED';
const WEBSOCKET_DISCONNECTED = 'WEBSOCKET_DISCONNECTED';

function websocketConnectedAction() {
  return {
    type: WEBSOCKET_CONNECTED,
    date: new Date()
  };
}

function websocketDisconnectedAction() {
  return {
    type: WEBSOCKET_DISCONNECTED,
    date: new Date()
  };
}

function WebSocketLabel(props) {
  const {websocketState} = props;
  switch (websocketState) {
    case WEBSOCKET_CONNECTING:
      return <ReactBootstrap.Label bsStyle="warning">WebSocket Connecting</ReactBootstrap.Label>;
    case WEBSOCKET_CONNECTED:
      return <ReactBootstrap.Label bsStyle="info">WebSocket Connected</ReactBootstrap.Label>;
    case WEBSOCKET_DISCONNECTED:
      return <ReactBootstrap.Label bsStyle="danger">WebSocket Disconnected</ReactBootstrap.Label>;
  }
}

const WebSocketLabelView = ReactRedux.connect(
  state => {
    return {websocketState: state.get('websocketState')};
  },
//dispatch => dispatch(someAction)
)(WebSocketLabel);


function killJob(jobId) {
  fetch(`${baseUrl}/job/${jobId}`, {method: 'DELETE'})
    .then(response => console.debug("Kill job - DELETE response:", response))
    .catch(e => console.error('Failed to kill job', e));
}

function calcPercentage(curr, total) {
  if (typeof total === 'number' && typeof curr === 'number' && total > 0) {
    return Math.floor(100 * curr / total);
  }
  return null;
}

function JobItem(props) {
  const {jobInfo} = props;
  const jobId = jobInfo.get('jobId'),
    description = jobInfo.get('description'),
    curr = jobInfo.get('curr'),
    total = jobInfo.get('total'),
    error = jobInfo.get('error'),
    startDateTime = jobInfo.get('startDateTime'),
    endDateTime = jobInfo.get('endDateTime'),
    percentage = calcPercentage(curr, total);
  return (
    <ReactBootstrap.ListGroupItem header={jobId + ' - ' + description}>
      {startDateTime && <ReactBootstrap.Label bsStyle="info">Started: {startDateTime}</ReactBootstrap.Label>}
      {endDateTime &&
      <span> <ReactBootstrap.Label bsStyle="info">Completed: {endDateTime}</ReactBootstrap.Label></span>}
      {error && <span> <ReactBootstrap.Label bsStyle="danger">Failed: {error}</ReactBootstrap.Label></span>}
      <a onClick={() => killJob(jobId)} className="close align-text-top" href="#">&times;</a>
      {(typeof percentage === 'number') ?
        <ReactBootstrap.ProgressBar
          label={percentage + '%'}
          active={endDateTime === undefined}
          now={percentage}/>
        : (typeof curr !== 'undefined') ?
          <ReactBootstrap.ProgressBar
            label={curr + ' done'}
            active={endDateTime === undefined}
            now={100}/>
          : <ReactBootstrap.ProgressBar/>
      }
    </ReactBootstrap.ListGroupItem>
  );
}

function JobList(props) {
  const {wsState, className} = props;
  return (
    <ReactBootstrap.ListGroup className={className}>
      {wsState.map((jobInfo) => <JobItem key={jobInfo.get('jobId')} jobInfo={jobInfo}/>).valueSeq()}
    </ReactBootstrap.ListGroup>
  );
}

const JobListComponent = ReactRedux.connect(
  state => {
    return {wsState: state.get('wsState')};
  },
  //dispatch => dispatch(someAction)
)(JobList);

const WEBSOCKET_STATE_DELETE = 'WEBSOCKET_STATE_DELETE';
const WEBSOCKET_STATE_UPDATE = 'WEBSOCKET_STATE_UPDATE';

function webSocketStateUpdateAction(data) {
  return {
    type: WEBSOCKET_STATE_UPDATE,
    updates: data,
  };
}

function webSocketStateDeleteAction(data) {
  return {
    type: WEBSOCKET_STATE_DELETE,
    deletes: data,
  };
}

const initialState = Immutable.fromJS({
  websocketState: WEBSOCKET_CONNECTING,
  wsState: Immutable.Map(),
});

function jobsApp(state = initialState, action) {
  switch (action.type) {
    case WEBSOCKET_STATE_DELETE:
      return state.set('wsState', action.deletes.reduce((acc, value) => acc.deleteIn(value), state.get('wsState')));

    case WEBSOCKET_STATE_UPDATE:
      return state.set('wsState', state.get('wsState').mergeDeep(action.updates).sort());

    case WEBSOCKET_CONNECTED:
      return state.set('websocketState', WEBSOCKET_CONNECTED);

    case WEBSOCKET_DISCONNECTED:
      return state.set('websocketState', WEBSOCKET_DISCONNECTED);

    default:
      return state
  }
}

class CreateJobsForm extends React.Component {
  constructor(props) {
    super(props);
    this.state = {
      description: 'Job Description here',
      total: 1000
    };

    this.handleInputChange = this.handleInputChange.bind(this);
    this.postJobCreate = this.postJobCreate.bind(this);
  }

  handleInputChange(event) {
    const target = event.target;
    const value = target.type === 'checkbox' ? target.checked : target.value;
    const name = target.name;

    this.setState({[name]: value});
  }

  static handleFocus(event) {
    event.target.select();
  }

  postJobCreate() {
    fetch(`${baseUrl}/job?description=${this.state.description}&total=${this.state.total}`, {method: 'POST'})
      .catch(e => console.error('Failed to create job', e));
  }

  render() {
    return (
      <ReactBootstrap.Navbar.Form pullLeft>
        <ReactBootstrap.FormGroup>
          <ReactBootstrap.FormControl type="text" placeholder="Description" name="description"
                                      value={this.state.description}
                                      onChange={this.handleInputChange}
                                      onFocus={CreateJobsForm.handleFocus}/>
          <ReactBootstrap.FormControl type="number" placeholder="Total" name="total"
                                      value={this.state.total}
                                      onChange={this.handleInputChange}
                                      onFocus={CreateJobsForm.handleFocus}/>
        </ReactBootstrap.FormGroup>{' '}
        <ReactBootstrap.Button type="submit" onClick={this.postJobCreate}>Create</ReactBootstrap.Button>
      </ReactBootstrap.Navbar.Form>
    );
  }
}

const store = Redux.createStore(jobsApp, initialState);

function extractUpdateAndDeletes(delta) {

  let deletes = [];

  function traverseObject(path, obj) {
    for (let i in obj) {
      const curr = obj[i];
      if (curr === null) {
        deletes.push(path.concat([i]));
        delete obj[i];
      } else if (typeof curr === 'object') {
        traverseObject(path.concat([i]), curr);
      }
    }
  }

  traverseObject([], delta);

  return [delta, deletes];
}

function receiveWebSocketFrame(msg) {
  const [delta, deletes] = extractUpdateAndDeletes(msg);
  if (deletes.length > 0) store.dispatch(webSocketStateDeleteAction(deletes));
  store.dispatch(webSocketStateUpdateAction(delta));
}

ReactDOM.render(
  <ReactRedux.Provider store={store}>
    <main className="main-container">

      <ReactBootstrap.Navbar className="navbar-row" defaultExpanded>
        <ReactBootstrap.Navbar.Header>
          <ReactBootstrap.Navbar.Brand>Job Runner</ReactBootstrap.Navbar.Brand>
          <ReactBootstrap.Navbar.Toggle/>
        </ReactBootstrap.Navbar.Header>
        <ReactBootstrap.Navbar.Collapse>
          <CreateJobsForm/>
          <ReactBootstrap.Nav pullRight>
            <ReactBootstrap.NavItem eventKey={1} href='api-docs/swagger.json'
                                    target='_blank'>api-docs/swagger.json</ReactBootstrap.NavItem>
            <ReactBootstrap.NavItem eventKey={2}><WebSocketLabelView/></ReactBootstrap.NavItem>
          </ReactBootstrap.Nav>
        </ReactBootstrap.Navbar.Collapse>
      </ReactBootstrap.Navbar>

      <JobListComponent className="joblist-row"/>

    </main>
  </ReactRedux.Provider>,
  document.querySelector('#app')
);

const subject = Rx.Observable.webSocket({
  url: wsUrl,
  openObserver: {
    next: () => {
      console.info(`[${new Date().toISOString()}] WebSocket connected`);
      store.dispatch(websocketConnectedAction());
    }
  },
  closeObserver: {
    next: () => {
      console.info(`[${new Date().toISOString()}] WebSocket disconnected`);
      store.dispatch(websocketDisconnectedAction());
    }
  },
});
console.info(`Connecting to WebSocket ${wsUrl}`);
subject.subscribe(
  receiveWebSocketFrame,
  (err) => console.error('WebSocket error:', err),
  () => console.info('WebSocket complete')
);

/*
// Example sending WebSocket frame
subject.next(JSON.stringify({op: 'hello'}));

// Example REST call to populate list
fetch(`${baseUrl}/job`)
    .then(response => response.json())
    .then(jobInfoArray => jobInfoArray.map(jobInfo => [jobInfo.jobId, jobInfo]))
    .then(jobsUpdateAction)
    .then(store.dispatch)
    .catch(e => console.error('Failed to fetch job list', e));
*/

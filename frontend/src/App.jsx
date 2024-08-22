import EnterUrl from './components/EnterUrl';
// import Result from './components/Result';
import { Route , Routes} from 'react-router-dom';

function App() {

    // API
    return (
        <Routes>
            <Route path="/" element={<EnterUrl/>}/>
            {/* <Route path="/result" element={<Result code={code} diffs={diffs}/>}/> */}
        </Routes>
    );
}

export default App;

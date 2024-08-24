import MainPage from './components/MainPage';
import { Route , Routes} from 'react-router-dom';
import ShowMethodDiff from './components/ShowMethodDiff';

function App() {

    // Routes
    return (
        <Routes>
            <Route path="/" element={<MainPage />} />
            <Route path="/ShowMethodDiff/*" element={<ShowMethodDiff />} />
        </Routes>
    );
}

export default App;

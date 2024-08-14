import React from 'react';
import {createRoot} from 'react-dom/client';
import App from './App';
import { BrowserRouter, Route, Routes} from "react-router-dom";

const container = document.getElementById('root');

const root = createRoot(container);

root.render(
    <React.StrictMode>
        <BrowserRouter>
            <Routes>
                <Route path="/*" element={<App />} />
            </Routes>
        </BrowserRouter>
    </React.StrictMode>
);

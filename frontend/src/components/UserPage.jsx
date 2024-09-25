import { Navigate } from "react-router-dom";
import { useCookies } from "react-cookie";

function UserPage() {
    const [cookies] = useCookies(['user']);
    if (!cookies.user) {
        return <Navigate to="/login" replace />;
    }

    return (
        <div>
            <h1>歡迎，{cookies.user.myUser.userName}</h1>
        </div>
    )
}

export default UserPage;
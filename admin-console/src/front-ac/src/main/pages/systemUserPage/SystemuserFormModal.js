import FormModal from "../../../components/modal/FormModal";
import SystemUserForm from "./SystemUserForm";
import useCreateSystemuser from "./useCreateSystemuser";
import {ModalOpenMode} from "../../../utils/constants";
import useUpdateSystemUser from "./useUpdateSystemUser";


const SystemUserModal = ({data, open, news, onClose, modalOpenMode, submitButton, ...props}) => {
    const {mutateAsync: onUpdate} = useUpdateSystemUser();
    const {mutateAsync: onCreate} = useCreateSystemuser();

    const initialValues = {
        id: data?.id ? data.id : 0,
        username: data?.username ? data.username : '',
        academyClasses: data?.academyClassList ? data.academyClassList : [],
        systemGroup: data?.groups ? data.groups : [],
        name: data?.name ? data.name : '',
        password: data?.password ? data.password : '',
        email: data?.email ? data.email : ''
    };

    return (
        <FormModal
            open={open}
            title={"დამატება"}
            cancelText={'დახურვა'}
            saveText={'შენახვა'}
            width={700}
            height={400}
            maxWidth={700}
            maxHeight={400}
            initialValues={

                {
                    ...initialValues
                }
            }
            formProps={{
                modalOpenMode
            }}
            form={SystemUserForm}
            // validationSchema={validateNewsFormData(!!news, t)}
            onSubmit={modalOpenMode === ModalOpenMode.add ? onCreate : onUpdate}
            onClose={onClose}
            submitButton={submitButton}
            {...props}
        />
    );
};

export default SystemUserModal;